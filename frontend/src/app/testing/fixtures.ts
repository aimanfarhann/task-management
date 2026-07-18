import { ComponentFixture } from '@angular/core/testing';
import { MemberDto, ProjectDto } from '../core/api/models';
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
