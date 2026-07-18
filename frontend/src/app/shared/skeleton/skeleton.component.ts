import { Component, input } from '@angular/core';

/**
 * Static skeleton placeholder block for initial page loads (DESIGN.md §7).
 * Intentionally not animated: DESIGN.md §6 forbids looping animation.
 */
@Component({
  selector: 'tf-skeleton',
  template: '',
  host: {
    '[style.width]': 'width()',
    '[style.height]': 'height()',
  },
  styles: `
    :host {
      display: block;
      border-radius: 12px;
      background-color: var(--mat-sys-surface-container-high);
    }
  `,
})
export class SkeletonComponent {
  readonly width = input('100%');
  readonly height = input('16px');
}
