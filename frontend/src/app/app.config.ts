import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { provideIcons } from '@ng-icons/core';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { appIcons } from './shared/icons';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    // Register the app's Lucide icon set once for every <ng-icon> in the tree.
    provideIcons(appIcons),
    // Native Date adapter backs the task due-date picker (MatDatepicker).
    provideNativeDateAdapter(),
    // DESIGN.md §7: outline appearance for every form field, set once.
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline' } },
  ],
};
