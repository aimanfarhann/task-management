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
      /* Glyph sits in a hairline-framed tonal badge — refined, not floating (DESIGN §5). */
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 72px;
      height: 72px;
      font-size: 34px;
      border-radius: var(--mat-sys-corner-large);
      border: 1px solid var(--tf-hairline);
      background-color: var(--mat-sys-surface-container);
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
