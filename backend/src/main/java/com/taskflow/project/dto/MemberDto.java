package com.taskflow.project.dto;

import com.taskflow.project.ProjectRole;
import java.time.Instant;

/**
 * API representation of a project member (API contract {@code MemberDto}).
 *
 * @param userId the member's user id
 * @param email the member's email address
 * @param displayName the member's display name
 * @param projectRole the member's role within the project
 * @param joinedAt when the user joined, serialized as an ISO-8601 string
 */
public record MemberDto(
    Long userId, String email, String displayName, ProjectRole projectRole, Instant joinedAt) {}
