import { Component, input, output } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmButton } from '@spartan-ng/helm/button';

/**
 * Empty state per DESIGN.md §7: icon + one sentence + primary action. The
 * `icon` input is a Lucide name registered in the app icon set (see icons.ts).
 */
@Component({
  selector: 'tf-empty-state',
  imports: [NgIcon, HlmButton],
  template: `
    <div class="flex flex-col items-center gap-4 px-6 py-12 text-center">
      <span
        aria-hidden="true"
        class="inline-flex size-[72px] items-center justify-center rounded-2xl border border-[var(--tf-hairline)] bg-muted text-muted-foreground"
      >
        <ng-icon [name]="icon()" size="2.125rem" />
      </span>
      <p class="m-0 max-w-[72ch] text-sm text-muted-foreground">{{ message() }}</p>
      @if (actionLabel(); as label) {
        <button hlmBtn type="button" (click)="action.emit()">{{ label }}</button>
      }
    </div>
  `,
})
export class EmptyStateComponent {
  readonly icon = input.required<string>();
  readonly message = input.required<string>();
  readonly actionLabel = input<string | null>(null);
  readonly action = output<void>();
}
