import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { applyFieldErrors, toApiErrorBody } from '../../core/api/api-error';
import { AuthStore } from '../../core/auth/auth.store';

@Component({
  selector: 'tf-login-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss',
})
export class LoginPageComponent {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly authStore = inject(AuthStore);
  private readonly router = inject(Router);

  protected readonly form = this.formBuilder.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      await this.authStore.login(this.form.getRawValue());
      await this.router.navigate(['/projects']);
    } catch (error) {
      const apiError = toApiErrorBody(error);
      if (apiError.code === 'INVALID_CREDENTIALS') {
        this.errorMessage.set('Email or password is incorrect.');
      } else if (!applyFieldErrors(this.form, apiError)) {
        this.errorMessage.set(apiError.message);
      }
    } finally {
      this.submitting.set(false);
    }
  }
}
