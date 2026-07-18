import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { toApiErrorBody } from '../../core/api/api-error';
import { CreateTaskRequest, TaskDto, TaskStatus, UpdateTaskRequest } from '../../core/api/models';
import { TasksApiClient } from '../../core/api/tasks-api.client';

/**
 * Signal store for one project's tasks. Holds the flat task list plus loading
 * and error state, and exposes the three status columns as computed views for
 * the board. Load swallows errors into the error signal for page-level
 * rendering; mutations rethrow so dialogs can map field errors onto forms.
 * Status changes apply optimistically and roll back on API failure (the caller
 * surfaces the failure, matching how M1 components own user feedback).
 */
@Injectable({ providedIn: 'root' })
export class TaskStore {
  private readonly tasksApi = inject(TasksApiClient);

  readonly tasks = signal<TaskDto[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly todoTasks = computed(() => this.tasks().filter((task) => task.status === 'TODO'));
  readonly inProgressTasks = computed(() =>
    this.tasks().filter((task) => task.status === 'IN_PROGRESS'),
  );
  readonly doneTasks = computed(() => this.tasks().filter((task) => task.status === 'DONE'));

  async loadTasks(projectId: number): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    this.tasks.set([]);
    try {
      this.tasks.set(await firstValueFrom(this.tasksApi.listTasks(projectId)));
    } catch (error) {
      this.error.set(toApiErrorBody(error).message);
    } finally {
      this.loading.set(false);
    }
  }

  async createTask(projectId: number, request: CreateTaskRequest): Promise<TaskDto> {
    const created = await firstValueFrom(this.tasksApi.createTask(projectId, request));
    this.tasks.update((tasks) => [...tasks, created]);
    return created;
  }

  async updateTask(
    projectId: number,
    taskId: number,
    request: UpdateTaskRequest,
  ): Promise<TaskDto> {
    const updated = await firstValueFrom(this.tasksApi.updateTask(projectId, taskId, request));
    this.replaceTask(updated);
    return updated;
  }

  async deleteTask(projectId: number, taskId: number): Promise<void> {
    await firstValueFrom(this.tasksApi.deleteTask(projectId, taskId));
    this.tasks.update((tasks) => tasks.filter((task) => task.id !== taskId));
  }

  /**
   * Optimistically moves a task to `status`, reconciling with the server copy
   * on success and, on failure, rolling back only that task's status (then
   * rethrowing) so the caller can notify the user. Rolling back a single task
   * rather than a whole-list snapshot preserves any concurrent create/delete or
   * other status change that landed while this request was in flight.
   */
  async changeStatus(projectId: number, taskId: number, status: TaskStatus): Promise<void> {
    const target = this.tasks().find((task) => task.id === taskId);
    if (!target || target.status === status) {
      return;
    }
    const previousStatus = target.status;
    this.setStatus(taskId, status);
    try {
      const updated = await firstValueFrom(
        this.tasksApi.updateStatus(projectId, taskId, { status }),
      );
      this.replaceTask(updated);
    } catch (error) {
      this.setStatus(taskId, previousStatus);
      throw error;
    }
  }

  private setStatus(taskId: number, status: TaskStatus): void {
    this.tasks.update((tasks) =>
      tasks.map((task) => (task.id === taskId ? { ...task, status } : task)),
    );
  }

  private replaceTask(updated: TaskDto): void {
    this.tasks.update((tasks) => tasks.map((task) => (task.id === updated.id ? updated : task)));
  }
}
