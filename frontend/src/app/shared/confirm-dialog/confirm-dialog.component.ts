import { Component, inject } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { HlmButton } from '@spartan-ng/helm/button';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel: string;
  destructive?: boolean;
}

/**
 * Confirmation dialog for destructive actions (DESIGN.md §7). Rendered in the
 * CDK overlay; closes with `true` when confirmed, `false`/`undefined` otherwise.
 * Opened through {@link ConfirmationService} so the overlay config lives once.
 */
@Component({
  selector: 'tf-confirm-dialog',
  imports: [HlmButton],
  template: `
    <div class="tf-dialog w-[26rem] max-w-[calc(100vw-2rem)] p-6">
      <h2 id="tf-confirm-title" class="tf-display text-lg">{{ data.title }}</h2>
      <p id="tf-confirm-message" class="mt-2 max-w-[72ch] text-sm text-muted-foreground">
        {{ data.message }}
      </p>
      <div class="mt-6 flex justify-end gap-2">
        <button hlmBtn variant="ghost" type="button" (click)="close(false)">Cancel</button>
        <button
          hlmBtn
          [variant]="data.destructive ? 'destructive' : 'default'"
          type="button"
          (click)="close(true)"
        >
          {{ data.confirmLabel }}
        </button>
      </div>
    </div>
  `,
})
export class ConfirmDialogComponent {
  protected readonly data = inject<ConfirmDialogData>(DIALOG_DATA);
  private readonly dialogRef = inject<DialogRef<boolean>>(DialogRef);

  protected close(result: boolean): void {
    this.dialogRef.close(result);
  }
}
