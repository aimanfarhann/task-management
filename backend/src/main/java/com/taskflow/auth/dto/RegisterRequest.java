package com.taskflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body of {@code POST /auth/register}.
 *
 * @param email the desired account email, unique case-insensitively
 * @param password the raw password, 8 to 100 characters
 * @param displayName the user's display name, 1 to 100 characters
 */
public record RegisterRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 8, max = 100) String password,
    @NotBlank @Size(max = 100) String displayName) {}
