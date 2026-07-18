import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { toApiErrorBody } from '../../core/api/api-error';
import { CommentDto, CreateCommentRequest } from '../../core/api/models';
import { TasksApiClient } from '../../core/api/tasks-api.client';

/**
 * Signal store for the comments of a single task (scoped to whichever task the
 * detail dialog currently shows). Comments stay in chronological ascending
 * order: the API returns them ascending and new comments append to the end.
 */
@Injectable({ providedIn: 'root' })
export class CommentStore {
  private readonly tasksApi = inject(TasksApiClient);

  readonly comments = signal<CommentDto[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  /**
   * Monotonic token identifying the latest load. This store is a singleton, so a
   * slow response for a previously-opened task could otherwise overwrite the
   * comments of the task now shown; a stale response (token mismatch) is ignored.
   */
  private loadToken = 0;

  async loadComments(projectId: number, taskId: number): Promise<void> {
    const token = ++this.loadToken;
    this.loading.set(true);
    this.error.set(null);
    this.comments.set([]);
    try {
      const loaded = await firstValueFrom(this.tasksApi.listComments(projectId, taskId));
      if (token === this.loadToken) {
        this.comments.set(loaded);
      }
    } catch (error) {
      if (token === this.loadToken) {
        this.error.set(toApiErrorBody(error).message);
      }
    } finally {
      if (token === this.loadToken) {
        this.loading.set(false);
      }
    }
  }

  async addComment(
    projectId: number,
    taskId: number,
    request: CreateCommentRequest,
  ): Promise<CommentDto> {
    const created = await firstValueFrom(this.tasksApi.createComment(projectId, taskId, request));
    this.comments.update((comments) => [...comments, created]);
    return created;
  }

  async deleteComment(projectId: number, taskId: number, commentId: number): Promise<void> {
    await firstValueFrom(this.tasksApi.deleteComment(projectId, taskId, commentId));
    this.comments.update((comments) => comments.filter((comment) => comment.id !== commentId));
  }
}
