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
