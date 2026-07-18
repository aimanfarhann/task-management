import { Component, computed, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Sort, MatSortModule } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MemberDto, TaskDto, TaskPriority, TaskStatus } from '../../core/api/models';
import { DueDatePipe } from '../../shared/due-date.pipe';
import { isPastDue } from '../../shared/due-date.util';
import { PriorityBadgeComponent } from '../../shared/priority-badge/priority-badge.component';
import { StatusChipComponent } from '../../shared/status-chip/status-chip.component';
import { TASK_STATUS_OPTIONS } from './task-status-options';

type StatusFilter = TaskStatus | 'ALL';
type PriorityFilter = TaskPriority | 'ALL';
type AssigneeFilter = number | 'ALL' | 'UNASSIGNED';

const PRIORITY_RANK: Record<TaskPriority, number> = { LOW: 0, MEDIUM: 1, HIGH: 2 };
const NO_DUE_DATE_SORT_KEY = '9999-12-31';

const COMPARATORS: Record<string, (a: TaskDto, b: TaskDto) => number> = {
  title: (a, b) => a.title.localeCompare(b.title),
  priority: (a, b) => PRIORITY_RANK[a.priority] - PRIORITY_RANK[b.priority],
  dueDate: (a, b) =>
    (a.dueDate ?? NO_DUE_DATE_SORT_KEY).localeCompare(b.dueDate ?? NO_DUE_DATE_SORT_KEY),
  createdAt: (a, b) => a.createdAt.localeCompare(b.createdAt),
};

/**
 * Table view of a project's tasks with client-side status/priority/assignee
 * filtering and sorting by title, priority, due date, or created date. Reads
 * the flat task list from TaskStore; row actions are emitted for the container.
 */
@Component({
  selector: 'tf-task-list',
  imports: [
    DatePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatMenuModule,
    MatSelectModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule,
    DueDatePipe,
    PriorityBadgeComponent,
    StatusChipComponent,
  ],
  templateUrl: './task-list.component.html',
  styleUrl: './task-list.component.scss',
})
export class TaskListComponent {
  readonly tasks = input.required<TaskDto[]>();
  readonly members = input<MemberDto[]>([]);

  readonly openTask = output<TaskDto>();
  readonly editTask = output<TaskDto>();
  readonly removeTask = output<TaskDto>();
  readonly moveTask = output<{ task: TaskDto; status: TaskStatus }>();

  protected readonly statusOptions = TASK_STATUS_OPTIONS;
  protected readonly displayedColumns = [
    'title',
    'status',
    'priority',
    'assignee',
    'dueDate',
    'createdAt',
    'actions',
  ];

  protected readonly statusFilter = signal<StatusFilter>('ALL');
  protected readonly priorityFilter = signal<PriorityFilter>('ALL');
  protected readonly assigneeFilter = signal<AssigneeFilter>('ALL');
  protected readonly sort = signal<Sort>({ active: '', direction: '' });

  protected readonly rows = computed(() => {
    const status = this.statusFilter();
    const priority = this.priorityFilter();
    const assignee = this.assigneeFilter();
    const filtered = this.tasks().filter(
      (task) =>
        (status === 'ALL' || task.status === status) &&
        (priority === 'ALL' || task.priority === priority) &&
        this.matchesAssignee(task, assignee),
    );
    return this.sortRows(filtered, this.sort());
  });

  protected moveOptionsFor(task: TaskDto) {
    return TASK_STATUS_OPTIONS.filter((option) => option.status !== task.status);
  }

  protected isOverdue(task: TaskDto): boolean {
    return isPastDue(task.dueDate) && task.status !== 'DONE';
  }

  protected onSortChange(sort: Sort): void {
    this.sort.set(sort);
  }

  private matchesAssignee(task: TaskDto, filter: AssigneeFilter): boolean {
    if (filter === 'ALL') {
      return true;
    }
    if (filter === 'UNASSIGNED') {
      return task.assignee === null;
    }
    return task.assignee?.userId === filter;
  }

  private sortRows(rows: TaskDto[], sort: Sort): TaskDto[] {
    const comparator = COMPARATORS[sort.active];
    if (!sort.direction || !comparator) {
      return rows;
    }
    const direction = sort.direction === 'asc' ? 1 : -1;
    return [...rows].sort((a, b) => direction * comparator(a, b));
  }
}
