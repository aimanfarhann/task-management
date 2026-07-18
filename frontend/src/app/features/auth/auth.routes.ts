import { Routes } from '@angular/router';
import { LoginPageComponent } from './login-page.component';
import { RegisterPageComponent } from './register-page.component';

export const AUTH_ROUTES: Routes = [
  { path: 'login', component: LoginPageComponent },
  { path: 'register', component: RegisterPageComponent },
];
