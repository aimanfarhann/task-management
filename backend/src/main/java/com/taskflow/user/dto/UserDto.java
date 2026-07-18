package com.taskflow.user.dto;

import com.taskflow.user.Role;

/**
 * API representation of a user (API contract {@code UserDto}).
 *
 * @param id the user's database id
 * @param email the user's email address
 * @param displayName the user's display name
 * @param role the user's system role, serialized as {@code USER} or {@code ADMIN}
 */
public record UserDto(Long id, String email, String displayName, Role role) {}
