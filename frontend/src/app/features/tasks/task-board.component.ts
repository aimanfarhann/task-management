import { Component, Signal, inject, output } from '@angular/core';
import { CdkDrag, CdkDragDrop, CdkDropList, CdkDropListGroup } from '@angular/cdk/drag-drop';
import { TaskDto, TaskStatus } from '../../core/api/models';
import { StatusChipComponent } from '../../shared/status-chip/status-chip.component';
import { TaskCardComponent } from './task-card.component';
import { TaskStore } from './task.store';

interface BoardColumn {
  status: TaskStatus;
  label: string;
  tasks: Signal<TaskDto[]>;
}

/**
 * Three-column task board (DESIGN.md §4). Renders the status columns computed
 * by TaskStore. Moving a card — by CDK drag-and-drop or via the per-card
 * "Move to" menu (the keyboard alternative required by DESIGN.md §8) — emits
 * the requested status change for the container to apply optimistically.
 */
@Component({
  selector: 'tf-task-board',
  imports: [CdkDropListGroup, CdkDropList, CdkDrag, StatusChipComponent, TaskCardComponent],
  templateUrl: './task-board.component.html',
  styleUrl: './task-board.component.scss',
})
export class TaskBoardComponent {
  private readonly taskStore = inject(TaskStore);

  readonly openTask = output<TaskDto>();
  readonly editTask = output<TaskDto>();
  readonly removeTask = output<TaskDto>();
  readonly moveTask = output<{ task: TaskDto; status: TaskStatus }>();

  protected readonly columns: BoardColumn[] = [
    { status: 'TODO', label: 'To do', tasks: this.taskStore.todoTasks },
    { status: 'IN_PROGRESS', label: 'In progress', tasks: this.taskStore.inProgressTasks },
    { status: 'DONE', label: 'Done', tasks: this.taskStore.doneTasks },
  ];

  protected onDrop(event: CdkDragDrop<TaskStatus>): void {
    const task = event.item.data as TaskDto;
    this.requestMove(task, event.container.data);
  }

  protected requestMove(task: TaskDto, status: TaskStatus): void {
    if (task.status !== status) {
      this.moveTask.emit({ task, status });
    }
  }
}
