import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

const PRIORITY_ICONS: Record<string, string> = {
  HIGH: 'keyboard_double_arrow_up',
  MEDIUM: 'drag_handle',
  LOW: 'keyboard_arrow_down',
};

/**
 * Priority badge (DESIGN.md §2/§8): icon + text label, colored via the
 * --tf-priority-* tokens — never color alone. Accepts the raw priority string
 * (like tf-color-dot) so shared/ stays decoupled from core models.
 */
@Component({
  selector: 'tf-priority-badge',
  imports: [MatIconModule],
  template: `
    <span class="priority-badge" [class]="'priority-badge--' + priority().toLowerCase()">
      <mat-icon class="priority-badge__icon" aria-hidden="true">{{ icon() }}</mat-icon>
      <span class="cdk-visually-hidden">Priority:</span>
      <span class="priority-badge__label">{{ label() }}</span>
    </span>
  `,
  styles: `
    .priority-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font: var(--mat-sys-label-medium);
    }

    .priority-badge__icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }

    .priority-badge--high {
      color: var(--tf-priority-high);
    }

    .priority-badge--medium {
      color: var(--tf-priority-medium);
    }

    .priority-badge--low {
      color: var(--tf-priority-low);
    }
  `,
})
export class PriorityBadgeComponent {
  readonly priority = input.required<string>();

  protected readonly icon = computed(() => PRIORITY_ICONS[this.priority()] ?? 'remove');
  protected readonly label = computed(() => {
    const value = this.priority();
    return value.charAt(0) + value.slice(1).toLowerCase();
  });
}
