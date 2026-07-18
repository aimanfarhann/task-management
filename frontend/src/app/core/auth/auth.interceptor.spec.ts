import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { buildAuthResponse, seedStoredSession } from '../../testing/fixtures';
import { authInterceptor } from './auth.interceptor';
import { AuthStore } from './auth.store';

const UNAUTHORIZED_BODY = { code: 'UNAUTHORIZED', message: 'Unauthorized', fieldErrors: [] };
const UNAUTHORIZED_OPTIONS = { status: 401, statusText: 'Unauthorized' };

describe('authInterceptor', () => {
  let httpMock: HttpTestingController;
  let http: HttpClient;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    http = TestBed.inject(HttpClient);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('request_authenticated_attachesBearerToken', () => {
    seedStoredSession();

    http.get('/api/v1/projects').subscribe();
    const request = httpMock.expectOne('/api/v1/projects');

    expect(request.request.headers.get('Authorization')).toBe('Bearer access-1');
    request.flush([]);
  });

  it('request_unauthenticated_hasNoAuthorizationHeader', () => {
    http.get('/api/v1/projects').subscribe({ error: () => undefined });
    const request = httpMock.expectOne('/api/v1/projects');

    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush([]);
  });

  it('request_publicAuthEndpoint_hasNoAuthorizationHeader', () => {
    seedStoredSession();

    http.post('/api/v1/auth/login', {}).subscribe();
    const request = httpMock.expectOne('/api/v1/auth/login');

    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush(buildAuthResponse());
  });

  it('response401_refreshSucceeds_retriesWithRotatedToken', () => {
    seedStoredSession();
    let result: unknown;

    http.get('/api/v1/projects').subscribe((value) => (result = value));
    httpMock.expectOne('/api/v1/projects').flush(UNAUTHORIZED_BODY, UNAUTHORIZED_OPTIONS);

    const refreshRequest = httpMock.expectOne('/api/v1/auth/refresh');
    expect(refreshRequest.request.body).toEqual({ refreshToken: 'refresh-1' });
    refreshRequest.flush(buildAuthResponse({ accessToken: 'access-2', refreshToken: 'refresh-2' }));

    const retriedRequest = httpMock.expectOne('/api/v1/projects');
    expect(retriedRequest.request.headers.get('Authorization')).toBe('Bearer access-2');
    retriedRequest.flush([]);

    expect(result).toEqual([]);
    expect(TestBed.inject(AuthStore).refreshToken()).toBe('refresh-2');
  });

  it('concurrent401s_singleFlight_shareOneRefreshRequest', () => {
    seedStoredSession();

    http.get('/api/v1/projects').subscribe();
    http.get('/api/v1/projects/1').subscribe();
    httpMock.expectOne('/api/v1/projects').flush(UNAUTHORIZED_BODY, UNAUTHORIZED_OPTIONS);
    httpMock.expectOne('/api/v1/projects/1').flush(UNAUTHORIZED_BODY, UNAUTHORIZED_OPTIONS);

    const refreshRequests = httpMock.match('/api/v1/auth/refresh');
    expect(refreshRequests.length).toBe(1);
    refreshRequests[0].flush(
      buildAuthResponse({ accessToken: 'access-2', refreshToken: 'refresh-2' }),
    );

    const firstRetry = httpMock.expectOne('/api/v1/projects');
    const secondRetry = httpMock.expectOne('/api/v1/projects/1');
    expect(firstRetry.request.headers.get('Authorization')).toBe('Bearer access-2');
    expect(secondRetry.request.headers.get('Authorization')).toBe('Bearer access-2');
    firstRetry.flush([]);
    secondRetry.flush({});
  });

  it('response401_refreshFails_propagatesErrorAndSignsOut', () => {
    seedStoredSession();
    const navigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    let error: unknown;

    http.get('/api/v1/projects').subscribe({ error: (requestError) => (error = requestError) });
    httpMock.expectOne('/api/v1/projects').flush(UNAUTHORIZED_BODY, UNAUTHORIZED_OPTIONS);
    httpMock
      .expectOne('/api/v1/auth/refresh')
      .flush(
        { code: 'INVALID_REFRESH_TOKEN', message: 'Invalid refresh token', fieldErrors: [] },
        UNAUTHORIZED_OPTIONS,
      );

    expect(error).toBeInstanceOf(HttpErrorResponse);
    expect(TestBed.inject(AuthStore).isAuthenticated()).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('response401_onLoginEndpoint_doesNotTriggerRefresh', () => {
    let error: unknown;

    http
      .post('/api/v1/auth/login', { email: 'a@b.c', password: 'wrong' })
      .subscribe({ error: (requestError) => (error = requestError) });
    httpMock
      .expectOne('/api/v1/auth/login')
      .flush(
        { code: 'INVALID_CREDENTIALS', message: 'Invalid credentials', fieldErrors: [] },
        UNAUTHORIZED_OPTIONS,
      );

    expect(error).toBeInstanceOf(HttpErrorResponse);
    httpMock.expectNone('/api/v1/auth/refresh');
  });

  it('logout_carriesBearerButDoesNotRefreshOn401', () => {
    // Logout must send the Bearer (unlike login), but a 401 from an expired access token must be
    // terminal — refreshing here would mint and persist a new session, defeating sign-out.
    seedStoredSession();
    let error: unknown;

    http
      .post('/api/v1/auth/logout', { refreshToken: 'refresh-1' })
      .subscribe({ error: (requestError) => (error = requestError) });
    const request = httpMock.expectOne('/api/v1/auth/logout');
    expect(request.request.headers.get('Authorization')).toBe('Bearer access-1');
    request.flush(UNAUTHORIZED_BODY, UNAUTHORIZED_OPTIONS);

    expect(error).toBeInstanceOf(HttpErrorResponse);
    httpMock.expectNone('/api/v1/auth/refresh');
  });

  it('response403_passesThroughWithoutRefresh', () => {
    seedStoredSession();
    let error: unknown;

    http.get('/api/v1/projects/9').subscribe({ error: (requestError) => (error = requestError) });
    httpMock
      .expectOne('/api/v1/projects/9')
      .flush(
        { code: 'FORBIDDEN', message: 'Not a member', fieldErrors: [] },
        { status: 403, statusText: 'Forbidden' },
      );

    expect(error).toBeInstanceOf(HttpErrorResponse);
    httpMock.expectNone('/api/v1/auth/refresh');
  });
});
