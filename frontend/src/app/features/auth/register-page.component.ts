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
  selector: 'tf-register-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './register-page.component.html',
  styleUrl: './register-page.component.scss',
})
export class RegisterPageComponent {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly authStore = inject(AuthStore);
  private readonly router = inject(Router);

  protected readonly form = this.formBuilder.group({
    displayName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
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
      await this.authStore.register(this.form.getRawValue());
      await this.router.navigate(['/projects']);
    } catch (error) {
      const apiError = toApiErrorBody(error);
      if (apiError.code === 'DUPLICATE_EMAIL') {
        this.form.controls.email.setErrors({
          server: 'An account with this email already exists.',
        });
        this.form.controls.email.markAsTouched();
      } else if (!applyFieldErrors(this.form, apiError)) {
        this.errorMessage.set(apiError.message);
      }
    } finally {
      this.submitting.set(false);
    }
  }
}
