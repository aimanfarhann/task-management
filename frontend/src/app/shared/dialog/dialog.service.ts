import { Injectable, inject } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { ComponentType } from '@angular/cdk/portal';
import { firstValueFrom } from 'rxjs';

interface OpenOptions {
  /** Panel width; the panel also caps at 92vw so it never overflows small screens. */
  width?: string;
}

/**
 * Opens a modal component in the CDK overlay and resolves to whatever the
 * dialog closed with (or `undefined` if dismissed via backdrop/Escape).
 * Centralizes the shared overlay config — backdrop, sizing, focus — so each
 * feature dialog only describes its own content.
 */
@Injectable({ providedIn: 'root' })
export class DialogService {
  private readonly dialog = inject(Dialog);

  open<TResult, TData = unknown>(
    component: ComponentType<unknown>,
    data: TData,
    options?: OpenOptions,
  ): Promise<TResult | undefined> {
    const dialogRef = this.dialog.open<TResult, TData>(component, {
      data,
      width: options?.width ?? '32rem',
      maxWidth: '92vw',
      backdropClass: 'tf-overlay-backdrop',
      autoFocus: 'first-tabbable',
    });
    return firstValueFrom(dialogRef.closed);
  }
}
