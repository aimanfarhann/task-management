import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';
import { ShellComponent } from './core/layout/shell.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'projects' },
  {
    path: '',
    canActivate: [guestGuard],
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'projects',
        loadChildren: () =>
          import('./features/projects/projects.routes').then((m) => m.PROJECTS_ROUTES),
      },
    ],
  },
  { path: '**', redirectTo: 'projects' },
];
