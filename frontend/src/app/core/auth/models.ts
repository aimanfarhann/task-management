/** System-wide user role (API contract). */
export type Role = 'USER' | 'ADMIN';

/** UserDto from the M1 API contract. */
export interface UserDto {
  id: number;
  email: string;
  displayName: string;
  role: Role;
}

/** AuthResponse from the M1 API contract. */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: UserDto;
}

/** Request body for POST /auth/login. */
export interface LoginRequest {
  email: string;
  password: string;
}

/** Request body for POST /auth/register. */
export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

/** Request body for POST /auth/refresh. */
export interface RefreshTokenRequest {
  refreshToken: string;
}

/** Request body for POST /auth/logout. */
export interface LogoutRequest {
  refreshToken: string;
}
