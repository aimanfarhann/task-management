import { Component, computed, input, output } from '@angular/core';
import { CdkMenu, CdkMenuItem, CdkMenuTrigger } from '@angular/cdk/menu';
import { NgIcon } from '@ng-icons/core';
import { HlmButton } from '@spartan-ng/helm/button';
import { TaskDto, TaskStatus } from '../../core/api/models';
import { DueDatePipe } from '../../shared/due-date.pipe';
import { isPastDue } from '../../shared/due-date.util';
import { PriorityBadgeComponent } from '../../shared/priority-badge/priority-badge.component';
import { TASK_STATUS_OPTIONS } from './task-status-options';

/**
 * Presentational board task card. All actions are emitted for the container to
 * handle; the "Move to" menu items are the keyboard-accessible alternative to
 * drag-and-drop required by DESIGN.md §8.
 */
@Component({
  selector: 'tf-task-card',
  imports: [
    CdkMenuTrigger,
    CdkMenu,
    CdkMenuItem,
    NgIcon,
    HlmButton,
    DueDatePipe,
    PriorityBadgeComponent,
  ],
  templateUrl: './task-card.component.html',
  styleUrl: './task-card.component.scss',
})
export class TaskCardComponent {
  readonly task = input.required<TaskDto>();

  readonly open = output<void>();
  readonly edit = output<void>();
  readonly remove = output<void>();
  readonly moveTo = output<TaskStatus>();

  protected readonly overdue = computed(
    () => isPastDue(this.task().dueDate) && this.task().status !== 'DONE',
  );
  protected readonly moveOptions = computed(() =>
    TASK_STATUS_OPTIONS.filter((option) => option.status !== this.task().status),
  );
}
