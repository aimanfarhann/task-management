package com.taskflow.task.dto;

import com.taskflow.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PATCH /projects/{projectId}/tasks/{taskId}/status}, used by the board
 * drag-and-drop.
 *
 * @param status the new workflow status
 */
public record UpdateTaskStatusRequest(@NotNull TaskStatus status) {}
