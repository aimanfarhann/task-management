package com.taskflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body of {@code POST /auth/login}.
 *
 * @param email the account email
 * @param password the raw password
 */
public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
