import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, firstValueFrom, share, tap, throwError } from 'rxjs';
import { AuthApiClient } from '../api/auth-api.client';
import { AuthResponse, LoginRequest, RegisterRequest, UserDto } from './models';

const AUTH_STORAGE_KEY = 'taskflow.auth';

interface StoredSession {
  accessToken: string;
  refreshToken: string;
  user: UserDto;
}

function isStoredSession(value: unknown): value is StoredSession {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const candidate = value as Partial<StoredSession>;
  return (
    typeof candidate.accessToken === 'string' &&
    typeof candidate.refreshToken === 'string' &&
    typeof candidate.user === 'object' &&
    candidate.user !== null
  );
}

function readStoredSession(): StoredSession | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const parsed: unknown = JSON.parse(raw);
    return isStoredSession(parsed) ? parsed : null;
  } catch {
    // Corrupted storage entry — treat as signed out rather than crashing.
    return null;
  }
}

/**
 * Signal store for the authenticated session. The session (tokens + user) is
 * persisted in localStorage and rehydrated on construction; token refresh is
 * single-flight so concurrent 401s share one rotation.
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly authApi = inject(AuthApiClient);
  private readonly router = inject(Router);

  private readonly session = signal<StoredSession | null>(readStoredSession());
  private refreshInFlight: Observable<AuthResponse> | null = null;

  readonly currentUser = computed(() => this.session()?.user ?? null);
  readonly accessToken = computed(() => this.session()?.accessToken ?? null);
  readonly refreshToken = computed(() => this.session()?.refreshToken ?? null);
  readonly isAuthenticated = computed(() => this.session() !== null);

  async login(request: LoginRequest): Promise<void> {
    const response = await firstValueFrom(this.authApi.login(request));
    this.applySession(response);
  }

  async register(request: RegisterRequest): Promise<void> {
    const response = await firstValueFrom(this.authApi.register(request));
    this.applySession(response);
  }

  /** Revokes the refresh token server-side, clears the session, routes to /login. */
  async logout(): Promise<void> {
    const refreshToken = this.refreshToken();
    if (refreshToken) {
      try {
        await firstValueFrom(this.authApi.logout({ refreshToken }));
      } catch {
        // Local sign-out must succeed even when revocation fails (network down,
        // token already revoked) — the refresh token expires server-side anyway.
      }
    }
    this.clearSession();
    await this.router.navigate(['/login']);
  }

  /**
   * Rotates the tokens via POST /auth/refresh. Single-flight: concurrent
   * callers share the same in-flight request. On failure the session is
   * cleared and the user is routed to /login.
   */
  refreshSession(): Observable<AuthResponse> {
    if (this.refreshInFlight) {
      return this.refreshInFlight;
    }
    const refreshToken = this.refreshToken();
    if (!refreshToken) {
      this.handleRefreshFailure();
      return throwError(() => new Error('No refresh token available'));
    }
    this.refreshInFlight = this.authApi.refresh({ refreshToken }).pipe(
      tap((response) => this.applySession(response)),
      catchError((error: unknown) => {
        this.handleRefreshFailure();
        return throwError(() => error);
      }),
      finalize(() => {
        this.refreshInFlight = null;
      }),
      share(),
    );
    return this.refreshInFlight;
  }

  private handleRefreshFailure(): void {
    this.clearSession();
    void this.router.navigate(['/login']);
  }

  private applySession(response: AuthResponse): void {
    const session: StoredSession = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      user: response.user,
    };
    this.session.set(session);
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
  }

  private clearSession(): void {
    this.session.set(null);
    localStorage.removeItem(AUTH_STORAGE_KEY);
  }
}
