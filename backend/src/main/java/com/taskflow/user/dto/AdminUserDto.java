package com.taskflow.user.dto;

import com.taskflow.user.Role;
import java.time.Instant;

/**
 * API representation of a user for the admin user-management page (M3 admin contract). Exposes the
 * account's active flag and creation time, which the regular {@link UserDto} omits — but never the
 * password hash.
 *
 * @param id the user's database id
 * @param email the user's email address
 * @param displayName the user's display name
 * @param role the user's system role, serialized as {@code USER} or {@code ADMIN}
 * @param active whether the account is active; inactive accounts are rejected at the JWT filter
 * @param createdAt account creation timestamp, serialized as an ISO-8601 string
 */
public record AdminUserDto(
    Long id, String email, String displayName, Role role, boolean active, Instant createdAt) {}
