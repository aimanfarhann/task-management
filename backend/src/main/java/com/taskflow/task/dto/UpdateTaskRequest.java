package com.taskflow.task.dto;

import com.taskflow.task.TaskPriority;
import com.taskflow.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request body of {@code PUT /projects/{projectId}/tasks/{taskId}} — a full replacement of the
 * task's mutable fields.
 *
 * @param title the new title, 1 to 200 characters
 * @param description the new description, may be null
 * @param status the new workflow status
 * @param priority the new priority
 * @param dueDate the new due date ({@code yyyy-MM-dd}), may be null
 * @param assigneeId the new assignee user id, or null to unassign; must be a project member when
 *     present
 */
public record UpdateTaskRequest(
    @NotBlank @Size(max = 200) String title,
    String description,
    @NotNull TaskStatus status,
    @NotNull TaskPriority priority,
    LocalDate dueDate,
    Long assigneeId) {}
