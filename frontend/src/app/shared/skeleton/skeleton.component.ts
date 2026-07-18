import { Component, input } from '@angular/core';

/**
 * Skeleton placeholder block for initial page loads (DESIGN.md §7). A slow
 * shimmer signals "loading"; it is disabled under prefers-reduced-motion by the
 * global rule in styles.scss (DESIGN §6).
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
      border-radius: var(--mat-sys-corner-medium);
      background: linear-gradient(
        100deg,
        var(--mat-sys-surface-container) 30%,
        var(--mat-sys-surface-container-high) 50%,
        var(--mat-sys-surface-container) 70%
      );
      background-size: 200% 100%;
      animation: tf-skeleton-shimmer 1.4s ease-in-out infinite;
    }

    @keyframes tf-skeleton-shimmer {
      from {
        background-position: 200% 0;
      }
      to {
        background-position: -200% 0;
      }
    }
  `,
})
export class SkeletonComponent {
  readonly width = input('100%');
  readonly height = input('16px');
}
