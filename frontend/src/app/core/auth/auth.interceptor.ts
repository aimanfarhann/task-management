import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthStore } from './auth.store';

// Endpoints that authenticate by body, not by Bearer token — they must not receive the Bearer.
const NO_BEARER_PATHS = ['/auth/login', '/auth/register', '/auth/refresh'];

// Endpoints where a 401 is terminal and must NOT trigger a token refresh. Logout carries the Bearer
// (it is authenticated) but, if the access token is expired, a refresh here would rotate to a brand
// new session and re-persist it — leaving a live refresh token server-side and defeating sign-out.
const NO_REFRESH_PATHS = [...NO_BEARER_PATHS, '/auth/logout'];

function matchesAny(url: string, paths: string[]): boolean {
  return paths.some((path) => url.includes(path));
}

function withBearer(request: HttpRequest<unknown>, accessToken: string): HttpRequest<unknown> {
  return request.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } });
}

/**
 * Attaches the Bearer access token to API requests. On a 401 it attempts one
 * token refresh (shared across concurrent 401s via AuthStore's single-flight
 * refresh) and retries the original request with the rotated token. When the
 * refresh itself fails, AuthStore clears the session and routes to /login.
 * Auth endpoints are excluded from the refresh-and-retry path (see the path lists above).
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authStore = inject(AuthStore);
  const skipBearer = matchesAny(request.url, NO_BEARER_PATHS);
  const skipRefresh = matchesAny(request.url, NO_REFRESH_PATHS);
  const accessToken = authStore.accessToken();
  const outgoingRequest = accessToken && !skipBearer ? withBearer(request, accessToken) : request;

  return next(outgoingRequest).pipe(
    catchError((error: unknown) => {
      const isUnauthorized = error instanceof HttpErrorResponse && error.status === 401;
      if (!isUnauthorized || skipRefresh) {
        return throwError(() => error);
      }
      return authStore
        .refreshSession()
        .pipe(switchMap((response) => next(withBearer(request, response.accessToken))));
    }),
  );
};
