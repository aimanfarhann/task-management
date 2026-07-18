import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel: string;
  destructive?: boolean;
}

/**
 * Confirmation dialog for destructive actions (DESIGN.md §7). Closes with
 * `true` when confirmed, `false`/`undefined` otherwise.
 */
@Component({
  selector: 'tf-confirm-dialog',
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p id="tf-confirm-message" class="confirm-dialog__message">{{ data.message }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false">Cancel</button>
      <button
        mat-flat-button
        type="button"
        [mat-dialog-close]="true"
        [class.confirm-dialog__confirm--destructive]="data.destructive"
      >
        {{ data.confirmLabel }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .confirm-dialog__message {
      margin: 0;
      max-width: 72ch;
      font: var(--mat-sys-body-medium);
    }

    .confirm-dialog__confirm--destructive {
      // Destructive confirm uses the error role colors (DESIGN.md §2).
      --mdc-filled-button-container-color: var(--mat-sys-error);
      --mdc-filled-button-label-text-color: var(--mat-sys-on-error);
      --mat-filled-button-container-color: var(--mat-sys-error);
      --mat-filled-button-label-text-color: var(--mat-sys-on-error);
      --mat-button-filled-container-color: var(--mat-sys-error);
      --mat-button-filled-label-text-color: var(--mat-sys-on-error);
    }
  `,
})
export class ConfirmDialogComponent {
  protected readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
}
