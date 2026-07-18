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
      gap: 5px;
      padding: 3px 10px 3px 7px;
      border-radius: var(--mat-sys-corner-small);
      border: 1px solid transparent;
      font: var(--mat-sys-label-medium);
      font-weight: 600;
      letter-spacing: -0.005em;
      white-space: nowrap;
    }

    .status-chip__icon {
      font-size: 17px;
      width: 17px;
      height: 17px;
    }

    /* Tonal fill + same-hue hairline so each status reads by color AND shape,
       while the label text keeps its AA-tuned --tf-status-* color. */
    .status-chip--todo {
      color: var(--tf-status-todo);
      background-color: color-mix(in srgb, var(--tf-status-todo) 13%, transparent);
      border-color: color-mix(in srgb, var(--tf-status-todo) 28%, transparent);
    }

    .status-chip--in-progress {
      color: var(--tf-status-in-progress);
      background-color: color-mix(in srgb, var(--tf-status-in-progress) 13%, transparent);
      border-color: color-mix(in srgb, var(--tf-status-in-progress) 28%, transparent);
    }

    .status-chip--done {
      color: var(--tf-status-done);
      background-color: color-mix(in srgb, var(--tf-status-done) 13%, transparent);
      border-color: color-mix(in srgb, var(--tf-status-done) 28%, transparent);
    }
  `,
})
export class StatusChipComponent {
  readonly status = input.required<string>();

  protected readonly meta = computed(() => STATUS_META[this.status()] ?? UNKNOWN_STATUS);
}
