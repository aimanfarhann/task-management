package com.taskflow.auth.dto;

import com.taskflow.user.dto.UserDto;

/**
 * Response body of the register, login, and refresh endpoints (API contract {@code AuthResponse}).
 *
 * @param accessToken HS256-signed JWT, valid for 60 minutes
 * @param refreshToken opaque token, valid for 7 days, rotated on every refresh
 * @param user the authenticated user
 */
public record AuthResponse(String accessToken, String refreshToken, UserDto user) {}
