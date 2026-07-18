import { HttpErrorResponse } from '@angular/common/http';
import { FormGroup } from '@angular/forms';
import { ApiErrorBody } from './models';

const FALLBACK_ERROR: ApiErrorBody = {
  code: 'UNKNOWN_ERROR',
  message: 'Something went wrong. Try again.',
  fieldErrors: [],
};

function isApiErrorBody(value: unknown): value is ApiErrorBody {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const candidate = value as Partial<ApiErrorBody>;
  return typeof candidate.code === 'string' && typeof candidate.message === 'string';
}

/**
 * Narrows any thrown value to the contract error body
 * `{ code, message, fieldErrors }`, falling back to a generic error when the
 * failure did not come from the API (network error, unexpected shape).
 */
export function toApiErrorBody(error: unknown): ApiErrorBody {
  if (error instanceof HttpErrorResponse && isApiErrorBody(error.error)) {
    return {
      code: error.error.code,
      message: error.error.message,
      fieldErrors: Array.isArray(error.error.fieldErrors) ? error.error.fieldErrors : [],
    };
  }
  return FALLBACK_ERROR;
}

/**
 * Maps the contract's fieldErrors onto matching form controls as a `server`
 * validation error and returns whether at least one control was marked, so
 * callers can fall back to a form-level message otherwise.
 */
export function applyFieldErrors(form: FormGroup, apiError: ApiErrorBody): boolean {
  let applied = false;
  for (const fieldError of apiError.fieldErrors) {
    const control = form.get(fieldError.field);
    if (control) {
      control.setErrors({ ...control.errors, server: fieldError.message });
      control.markAsTouched();
      applied = true;
    }
  }
  return applied;
}
