import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CommentDto,
  CreateCommentRequest,
  CreateTaskRequest,
  TaskDto,
  TaskFilters,
  UpdateTaskRequest,
  UpdateTaskStatusRequest,
} from './models';

/** Typed wrapper for the /api/v1/projects/{projectId}/tasks endpoints (tasks + comments). */
@Injectable({ providedIn: 'root' })
export class TasksApiClient {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/projects';

  listTasks(projectId: number, filters: TaskFilters = {}): Observable<TaskDto[]> {
    let params = new HttpParams();
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.priority) {
      params = params.set('priority', filters.priority);
    }
    if (filters.assigneeId !== undefined) {
      params = params.set('assigneeId', filters.assigneeId);
    }
    return this.http.get<TaskDto[]>(this.tasksUrl(projectId), { params });
  }

  getTask(projectId: number, taskId: number): Observable<TaskDto> {
    return this.http.get<TaskDto>(this.taskUrl(projectId, taskId));
  }

  createTask(projectId: number, request: CreateTaskRequest): Observable<TaskDto> {
    return this.http.post<TaskDto>(this.tasksUrl(projectId), request);
  }

  updateTask(projectId: number, taskId: number, request: UpdateTaskRequest): Observable<TaskDto> {
    return this.http.put<TaskDto>(this.taskUrl(projectId, taskId), request);
  }

  updateStatus(
    projectId: number,
    taskId: number,
    request: UpdateTaskStatusRequest,
  ): Observable<TaskDto> {
    return this.http.patch<TaskDto>(`${this.taskUrl(projectId, taskId)}/status`, request);
  }

  deleteTask(projectId: number, taskId: number): Observable<void> {
    return this.http.delete<void>(this.taskUrl(projectId, taskId));
  }

  listComments(projectId: number, taskId: number): Observable<CommentDto[]> {
    return this.http.get<CommentDto[]>(this.commentsUrl(projectId, taskId));
  }

  createComment(
    projectId: number,
    taskId: number,
    request: CreateCommentRequest,
  ): Observable<CommentDto> {
    return this.http.post<CommentDto>(this.commentsUrl(projectId, taskId), request);
  }

  deleteComment(projectId: number, taskId: number, commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.commentsUrl(projectId, taskId)}/${commentId}`);
  }

  private tasksUrl(projectId: number): string {
    return `${this.baseUrl}/${projectId}/tasks`;
  }

  private taskUrl(projectId: number, taskId: number): string {
    return `${this.tasksUrl(projectId)}/${taskId}`;
  }

  private commentsUrl(projectId: number, taskId: number): string {
    return `${this.taskUrl(projectId, taskId)}/comments`;
  }
}
