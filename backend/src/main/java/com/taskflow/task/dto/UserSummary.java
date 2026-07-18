package com.taskflow.task.dto;

/**
 * Minimal API representation of a user, embedded in task and comment payloads (API contract {@code
 * UserSummary}).
 *
 * @param userId the user's database id
 * @param displayName the user's display name
 */
public record UserSummary(Long userId, String displayName) {}
