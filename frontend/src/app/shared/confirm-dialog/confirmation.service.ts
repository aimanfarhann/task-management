import { Injectable, inject } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { firstValueFrom } from 'rxjs';
import { ConfirmDialogComponent, ConfirmDialogData } from './confirm-dialog.component';

/**
 * Opens the shared confirmation dialog and resolves to the user's choice.
 * Centralizes the CDK overlay config (backdrop, a11y roles) so callers only
 * describe the decision: `if (await confirmation.confirm({...})) { ... }`.
 */
@Injectable({ providedIn: 'root' })
export class ConfirmationService {
  private readonly dialog = inject(Dialog);

  async confirm(data: ConfirmDialogData): Promise<boolean> {
    const dialogRef = this.dialog.open<boolean>(ConfirmDialogComponent, {
      data,
      backdropClass: 'tf-overlay-backdrop',
      role: 'alertdialog',
      ariaLabelledBy: 'tf-confirm-title',
      ariaDescribedBy: 'tf-confirm-message',
    });
    const result = await firstValueFrom(dialogRef.closed);
    return result === true;
  }
}
