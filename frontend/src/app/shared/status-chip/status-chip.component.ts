import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

interface StatusMeta {
  label: string;
  icon: string;
  modifier: string;
}

const STATUS_META: Record<string, StatusMeta> = {
  TODO: { label: 'To do', icon: 'radio_button_unchecked', modifier: 'todo' },
  IN_PROGRESS: { label: 'In progress', icon: 'timelapse', modifier: 'in-progress' },
  DONE: { label: 'Done', icon: 'check_circle', modifier: 'done' },
};

const UNKNOWN_STATUS: StatusMeta = { label: 'Unknown', icon: 'help', modifier: 'todo' };

/**
 * Status chip (DESIGN.md §2/§8): icon + text label, colored via the
 * --tf-status-* tokens — never color alone. Accepts the raw status string so
 * shared/ stays decoupled from core models.
 */
@Component({
  selector: 'tf-status-chip',
  imports: [MatIconModule],
  template: `
    <span class="status-chip" [class]="'status-chip--' + meta().modifier">
      <mat-icon class="status-chip__icon" aria-hidden="true">{{ meta().icon }}</mat-icon>
      <span class="cdk-visually-hidden">Status:</span>
      <span class="status-chip__label">{{ meta().label }}</span>
    </span>
  `,
  styles: `
    .status-chip {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font: var(--mat-sys-label-medium);
    }

    .status-chip__icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }

    .status-chip--todo {
      color: var(--tf-status-todo);
    }

    .status-chip--in-progress {
      color: var(--tf-status-in-progress);
    }

    .status-chip--done {
      color: var(--tf-status-done);
    }
  `,
})
export class StatusChipComponent {
  readonly status = input.required<string>();

  protected readonly meta = computed(() => STATUS_META[this.status()] ?? UNKNOWN_STATUS);
}
