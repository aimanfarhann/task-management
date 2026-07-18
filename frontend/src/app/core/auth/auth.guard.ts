import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from './auth.store';

/** Blocks unauthenticated access to app routes; redirects to /login. */
export const authGuard: CanActivateFn = () => {
  const authStore = inject(AuthStore);
  const router = inject(Router);
  return authStore.isAuthenticated() ? true : router.createUrlTree(['/login']);
};

/** Keeps authenticated users off the login/register pages; redirects to /projects. */
export const guestGuard: CanActivateFn = () => {
  const authStore = inject(AuthStore);
  const router = inject(Router);
  return authStore.isAuthenticated() ? router.createUrlTree(['/projects']) : true;
};

/**
 * Restricts admin-only routes to ADMIN users; sends an authenticated non-admin
 * to /dashboard. Runs after authGuard, so an unauthenticated caller is already
 * redirected to /login before this check applies.
 */
export const adminGuard: CanActivateFn = () => {
  const authStore = inject(AuthStore);
  const router = inject(Router);
  return authStore.currentUser()?.role === 'ADMIN' ? true : router.createUrlTree(['/dashboard']);
};
