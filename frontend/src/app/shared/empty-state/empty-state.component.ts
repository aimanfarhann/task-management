import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/** Empty state per DESIGN.md §7: icon + one sentence + primary action. */
@Component({
  selector: 'tf-empty-state',
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="empty-state">
      <mat-icon aria-hidden="true" class="empty-state__icon">{{ icon() }}</mat-icon>
      <p class="empty-state__message">{{ message() }}</p>
      @if (actionLabel(); as label) {
        <button mat-flat-button type="button" (click)="action.emit()">{{ label }}</button>
      }
    </div>
  `,
  styles: `
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      padding: 48px 24px;
      text-align: center;
    }

    .empty-state__icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: var(--mat-sys-on-surface-variant);
    }

    .empty-state__message {
      margin: 0;
      max-width: 72ch;
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-body-medium);
    }
  `,
})
export class EmptyStateComponent {
  readonly icon = input.required<string>();
  readonly message = input.required<string>();
  readonly actionLabel = input<string | null>(null);
  readonly action = output<void>();
}
