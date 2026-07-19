import { Component, computed, input } from '@angular/core';
import { NgIcon } from '@ng-icons/core';

const PRIORITY_ICONS: Record<string, string> = {
  HIGH: 'lucideChevronsUp',
  MEDIUM: 'lucideEqual',
  LOW: 'lucideChevronDown',
};

/**
 * Priority badge (DESIGN.md §2/§8): icon + text label, colored via the
 * --tf-priority-* tokens — never color alone. Accepts the raw priority string
 * (like tf-color-dot) so shared/ stays decoupled from core models.
 */
@Component({
  selector: 'tf-priority-badge',
  imports: [NgIcon],
  template: `
    <span class="priority-badge" [class]="'priority-badge--' + priority().toLowerCase()">
      <ng-icon [name]="icon()" size="1rem" aria-hidden="true" />
      <span class="sr-only">Priority:</span>
      <span class="priority-badge__label">{{ label() }}</span>
    </span>
  `,
  styles: `
    .priority-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 3px 9px 3px 6px;
      border-radius: 6px;
      border: 1px solid transparent;
      font-size: 0.75rem;
      line-height: 1rem;
      font-weight: 600;
      letter-spacing: -0.005em;
      white-space: nowrap;
    }

    /* Tonal fill + same-hue hairline: the badge reads by color and shape at a
       glance on the board; text keeps its AA-tuned --tf-priority-* color. */
    .priority-badge--high {
      color: var(--tf-priority-high);
      background-color: color-mix(in srgb, var(--tf-priority-high) 14%, transparent);
      border-color: color-mix(in srgb, var(--tf-priority-high) 30%, transparent);
    }

    .priority-badge--medium {
      color: var(--tf-priority-medium);
      background-color: color-mix(in srgb, var(--tf-priority-medium) 14%, transparent);
      border-color: color-mix(in srgb, var(--tf-priority-medium) 30%, transparent);
    }

    .priority-badge--low {
      color: var(--tf-priority-low);
      background-color: color-mix(in srgb, var(--tf-priority-low) 13%, transparent);
      border-color: color-mix(in srgb, var(--tf-priority-low) 28%, transparent);
    }
  `,
})
export class PriorityBadgeComponent {
  readonly priority = input.required<string>();

  protected readonly icon = computed(() => PRIORITY_ICONS[this.priority()] ?? 'lucideMinus');
  protected readonly label = computed(() => {
    const value = this.priority();
    return value.charAt(0) + value.slice(1).toLowerCase();
  });
}
