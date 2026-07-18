import { ComponentFixture } from '@angular/core/testing';
import {
  CommentDto,
  DashboardDto,
  DashboardTaskDto,
  MemberDto,
  ProjectDto,
  ProjectSummaryDto,
  TaskDto,
  UserSummary,
} from '../core/api/models';
import { AuthResponse } from '../core/auth/models';

/**
 * Waits for pending store promises (microtasks) to finish and for the zoneless
 * change detection they schedule to render.
 */
export async function settle(fixture: ComponentFixture<unknown>): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 0));
  await fixture.whenStable();
}

export const AUTH_STORAGE_KEY = 'taskflow.auth';

export function buildAuthResponse(overrides: Partial<AuthResponse> = {}): AuthResponse {
  return {
    accessToken: 'access-1',
    refreshToken: 'refresh-1',
    user: { id: 1, email: 'ada@example.com', displayName: 'Ada Lovelace', role: 'USER' },
    ...overrides,
  };
}

/** Writes a valid session to localStorage so stores rehydrate as signed in. */
export function seedStoredSession(overrides: Partial<AuthResponse> = {}): void {
  const response = buildAuthResponse(overrides);
  localStorage.setItem(
    AUTH_STORAGE_KEY,
    JSON.stringify({
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      user: response.user,
    }),
  );
}

export function buildProject(overrides: Partial<ProjectDto> = {}): ProjectDto {
  return {
    id: 1,
    name: 'Apollo',
    description: 'Moonshot planning',
    colorTag: 'blue',
    archived: false,
    createdAt: '2026-07-01T10:00:00Z',
    myRole: 'OWNER',
    memberCount: 2,
    ...overrides,
  };
}

export function buildMember(overrides: Partial<MemberDto> = {}): MemberDto {
  return {
    userId: 1,
    email: 'ada@example.com',
    displayName: 'Ada Lovelace',
    projectRole: 'OWNER',
    joinedAt: '2026-07-01T10:00:00Z',
    ...overrides,
  };
}

export function buildUserSummary(overrides: Partial<UserSummary> = {}): UserSummary {
  return { userId: 1, displayName: 'Ada Lovelace', ...overrides };
}

export function buildTask(overrides: Partial<TaskDto> = {}): TaskDto {
  return {
    id: 1,
    projectId: 1,
    title: 'Write the spec',
    description: null,
    status: 'TODO',
    priority: 'MEDIUM',
    dueDate: null,
    assignee: null,
    createdBy: buildUserSummary(),
    createdAt: '2026-07-01T10:00:00Z',
    updatedAt: '2026-07-01T10:00:00Z',
    ...overrides,
  };
}

export function buildComment(overrides: Partial<CommentDto> = {}): CommentDto {
  return {
    id: 1,
    taskId: 1,
    author: buildUserSummary(),
    body: 'Looks good to me',
    createdAt: '2026-07-02T09:00:00Z',
    ...overrides,
  };
}

export function buildDashboardTask(overrides: Partial<DashboardTaskDto> = {}): DashboardTaskDto {
  return {
    ...buildTask(),
    assignee: buildUserSummary(),
    projectName: 'Apollo',
    projectColorTag: 'blue',
    ...overrides,
  };
}

export function buildProjectSummary(overrides: Partial<ProjectSummaryDto> = {}): ProjectSummaryDto {
  return {
    projectId: 1,
    projectName: 'Apollo',
    colorTag: 'blue',
    todoCount: 2,
    inProgressCount: 1,
    doneCount: 3,
    ...overrides,
  };
}

export function buildDashboard(overrides: Partial<DashboardDto> = {}): DashboardDto {
  return {
    myTasks: [buildDashboardTask()],
    projectSummaries: [buildProjectSummary()],
    ...overrides,
  };
}
