package com.taskflow.task.dto;

import java.time.Instant;

/**
 * API representation of a task comment (API contract {@code CommentDto}).
 *
 * @param id the comment's database id
 * @param taskId the commented task's id
 * @param author the comment author
 * @param body the comment text
 * @param createdAt creation timestamp, serialized as an ISO-8601 string
 */
public record CommentDto(
    Long id, Long taskId, UserSummary author, String body, Instant createdAt) {}
