import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AuthResponse,
  LoginRequest,
  LogoutRequest,
  RefreshTokenRequest,
  RegisterRequest,
} from '../auth/models';

/** Typed wrapper for the /api/v1/auth endpoints. */
@Injectable({ providedIn: 'root' })
export class AuthApiClient {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/auth';

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, request);
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, request);
  }

  refresh(request: RefreshTokenRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/refresh`, request);
  }

  logout(request: LogoutRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/logout`, request);
  }
}
