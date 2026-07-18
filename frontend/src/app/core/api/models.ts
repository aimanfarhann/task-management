import { Role } from '../auth/models';

/** The 8 preset project color tag keys (API contract / DESIGN.md §2). */
export const COLOR_TAGS = [
  'red',
  'orange',
  'amber',
  'green',
  'teal',
  'blue',
  'indigo',
  'purple',
] as const;

export type ColorTag = (typeof COLOR_TAGS)[number];

/** Role of a user within a project (API contract). */
export type ProjectRole = 'OWNER' | 'MEMBER';

/** ProjectDto from the M1 API contract. */
export interface ProjectDto {
  id: number;
  name: string;
  description: string | null;
  colorTag: ColorTag;
  archived: boolean;
  createdAt: string;
  myRole: ProjectRole;
  memberCount: number;
}

/** MemberDto from the M1 API contract. */
export interface MemberDto {
  userId: number;
  email: string;
  displayName: string;
  projectRole: ProjectRole;
  joinedAt: string;
}

/** Request body for POST /projects. */
export interface CreateProjectRequest {
  name: string;
  description?: string | null;
  colorTag: ColorTag;
}

/** Request body for PUT /projects/{id}. */
export interface UpdateProjectRequest {
  name: string;
  description: string | null;
  colorTag: ColorTag;
  archived: boolean;
}

/** Request body for POST /projects/{id}/members. `projectRole` defaults to MEMBER server-side. */
export interface AddMemberRequest {
  email: string;
  projectRole?: ProjectRole;
}

/** Task lifecycle status (API contract; maps to the tasks.status CHECK values). */
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

/** Task priority (API contract; maps to the tasks.priority CHECK values). */
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

/** Minimal user projection embedded in task/comment payloads (API contract). */
export interface UserSummary {
  userId: number;
  displayName: string;
}

/** TaskDto from the M2 API contract. */
export interface TaskDto {
  id: number;
  projectId: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  /** ISO date `yyyy-MM-dd`, or null when no due date is set. */
  dueDate: string | null;
  assignee: UserSummary | null;
  createdBy: UserSummary;
  createdAt: string;
  updatedAt: string;
}

/** CommentDto from the M2 API contract. */
export interface CommentDto {
  id: number;
  taskId: number;
  author: UserSummary;
  body: string;
  createdAt: string;
}

/** A dashboard task carries its owning project's name and color tag inline. */
export interface DashboardTaskDto extends TaskDto {
  projectName: string;
  projectColorTag: ColorTag;
}

/** Per-project status roll-up shown on the dashboard (API contract). */
export interface ProjectSummaryDto {
  projectId: number;
  projectName: string;
  colorTag: ColorTag;
  todoCount: number;
  inProgressCount: number;
  doneCount: number;
}

/** DashboardDto from the M2 API contract. */
export interface DashboardDto {
  myTasks: DashboardTaskDto[];
  projectSummaries: ProjectSummaryDto[];
}

/** Request body for POST /projects/{id}/tasks. `priority` defaults to MEDIUM server-side. */
export interface CreateTaskRequest {
  title: string;
  description?: string | null;
  priority?: TaskPriority;
  dueDate?: string | null;
  assigneeId?: number | null;
}

/** Request body for PUT /projects/{id}/tasks/{taskId} (full update). */
export interface UpdateTaskRequest {
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string | null;
  assigneeId: number | null;
}

/** Request body for PATCH /projects/{id}/tasks/{taskId}/status. */
export interface UpdateTaskStatusRequest {
  status: TaskStatus;
}

/** Optional server-side task filters for GET /projects/{id}/tasks. */
export interface TaskFilters {
  status?: TaskStatus;
  priority?: TaskPriority;
  assigneeId?: number;
}

/** Request body for POST /projects/{id}/tasks/{taskId}/comments. */
export interface CreateCommentRequest {
  body: string;
}

/** AdminUserDto from the M3 admin API contract. */
export interface AdminUserDto {
  id: number;
  email: string;
  displayName: string;
  role: Role;
  active: boolean;
  createdAt: string;
}

/** Request body for PATCH /admin/users/{userId}/active. */
export interface SetUserActiveRequest {
  active: boolean;
}

/** One field-level validation error inside an API error body. */
export interface FieldError {
  field: string;
  message: string;
}

/** The single error body shape every API endpoint returns. */
export interface ApiErrorBody {
  code: string;
  message: string;
  fieldErrors: FieldError[];
}
