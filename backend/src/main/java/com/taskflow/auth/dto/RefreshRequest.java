package com.taskflow.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body of {@code POST /auth/refresh} and {@code POST /auth/logout}.
 *
 * @param refreshToken the opaque refresh token value issued at login/refresh
 */
public record RefreshRequest(@NotBlank String refreshToken) {}
