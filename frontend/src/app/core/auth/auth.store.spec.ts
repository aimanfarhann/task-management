import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AUTH_STORAGE_KEY, buildAuthResponse, seedStoredSession } from '../../testing/fixtures';
import { AuthStore } from './auth.store';

describe('AuthStore', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('login_success_storesSessionAndPersistsIt', async () => {
    const store = TestBed.inject(AuthStore);

    const loginPromise = store.login({ email: 'ada@example.com', password: 'password123' });
    const request = httpMock.expectOne('/api/v1/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'ada@example.com', password: 'password123' });
    request.flush(buildAuthResponse());
    await loginPromise;

    expect(store.isAuthenticated()).toBe(true);
    expect(store.currentUser()?.displayName).toBe('Ada Lovelace');
    expect(store.accessToken()).toBe('access-1');
    const stored: unknown = JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY) ?? 'null');
    expect(stored).toEqual({
      accessToken: 'access-1',
      refreshToken: 'refresh-1',
      user: buildAuthResponse().user,
    });
  });

  it('register_success_storesSession', async () => {
    const store = TestBed.inject(AuthStore);

    const registerPromise = store.register({
      email: 'ada@example.com',
      password: 'password123',
      displayName: 'Ada Lovelace',
    });
    const request = httpMock.expectOne('/api/v1/auth/register');
    expect(request.request.method).toBe('POST');
    request.flush(buildAuthResponse());
    await registerPromise;

    expect(store.isAuthenticated()).toBe(true);
  });

  it('constructor_storedSession_rehydratesFromLocalStorage', () => {
    seedStoredSession();

    const store = TestBed.inject(AuthStore);

    expect(store.isAuthenticated()).toBe(true);
    expect(store.currentUser()?.email).toBe('ada@example.com');
    expect(store.refreshToken()).toBe('refresh-1');
  });

  it('constructor_corruptedStorage_staysSignedOut', () => {
    localStorage.setItem(AUTH_STORAGE_KEY, 'not-json');

    const store = TestBed.inject(AuthStore);

    expect(store.isAuthenticated()).toBe(false);
  });

  it('refreshSession_success_rotatesTokensAndSharesOneRequest', async () => {
    seedStoredSession();
    const store = TestBed.inject(AuthStore);

    const first = firstValueFrom(store.refreshSession());
    const second = firstValueFrom(store.refreshSession());
    const requests = httpMock.match('/api/v1/auth/refresh');
    expect(requests.length).toBe(1);
    expect(requests[0].request.body).toEqual({ refreshToken: 'refresh-1' });
    requests[0].flush(buildAuthResponse({ accessToken: 'access-2', refreshToken: 'refresh-2' }));
    await Promise.all([first, second]);

    expect(store.accessToken()).toBe('access-2');
    expect(store.refreshToken()).toBe('refresh-2');
    const stored: unknown = JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY) ?? 'null');
    expect(stored).toMatchObject({ refreshToken: 'refresh-2' });
  });

  it('refreshSession_failure_clearsSessionAndNavigatesToLogin', async () => {
    seedStoredSession();
    const store = TestBed.inject(AuthStore);
    const navigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    const refreshPromise = firstValueFrom(store.refreshSession());
    httpMock
      .expectOne('/api/v1/auth/refresh')
      .flush(
        { code: 'INVALID_REFRESH_TOKEN', message: 'Invalid refresh token', fieldErrors: [] },
        { status: 401, statusText: 'Unauthorized' },
      );
    await expect(refreshPromise).rejects.toBeTruthy();

    expect(store.isAuthenticated()).toBe(false);
    expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull();
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('refreshSession_noStoredToken_failsAndNavigatesToLogin', async () => {
    const store = TestBed.inject(AuthStore);
    const navigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    await expect(firstValueFrom(store.refreshSession())).rejects.toBeTruthy();

    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('logout_success_revokesTokenClearsSessionAndNavigates', async () => {
    seedStoredSession();
    const store = TestBed.inject(AuthStore);
    const navigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    const logoutPromise = store.logout();
    const request = httpMock.expectOne('/api/v1/auth/logout');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ refreshToken: 'refresh-1' });
    request.flush(null, { status: 204, statusText: 'No Content' });
    await logoutPromise;

    expect(store.isAuthenticated()).toBe(false);
    expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull();
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('logout_apiFailure_stillClearsSessionLocally', async () => {
    seedStoredSession();
    const store = TestBed.inject(AuthStore);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    const logoutPromise = store.logout();
    httpMock
      .expectOne('/api/v1/auth/logout')
      .flush(
        { code: 'UNKNOWN_ERROR', message: 'boom', fieldErrors: [] },
        { status: 500, statusText: 'Server Error' },
      );
    await logoutPromise;

    expect(store.isAuthenticated()).toBe(false);
    expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull();
  });
});
