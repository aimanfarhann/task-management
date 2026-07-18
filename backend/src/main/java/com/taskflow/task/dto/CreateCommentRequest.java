package com.taskflow.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body of {@code POST /projects/{projectId}/tasks/{taskId}/comments}.
 *
 * @param body the comment text, 1 to 2000 characters
 */
public record CreateCommentRequest(@NotBlank @Size(max = 2000) String body) {}
