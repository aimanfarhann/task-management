import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { seedStoredSession } from '../../testing/fixtures';
import { authGuard, guestGuard } from './auth.guard';

describe('auth guards', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
  });

  afterEach(() => {
    localStorage.clear();
  });

  function runGuard(guard: CanActivateFn): unknown {
    return TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
  }

  it('authGuard_authenticated_allowsActivation', () => {
    seedStoredSession();

    expect(runGuard(authGuard)).toBe(true);
  });

  it('authGuard_unauthenticated_redirectsToLogin', () => {
    const result = runGuard(authGuard);

    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toBe('/login');
  });

  it('guestGuard_unauthenticated_allowsActivation', () => {
    expect(runGuard(guestGuard)).toBe(true);
  });

  it('guestGuard_authenticated_redirectsToProjects', () => {
    seedStoredSession();

    const result = runGuard(guestGuard);

    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toBe('/projects');
  });
});
