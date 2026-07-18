package com.taskflow.task.dto;

import com.taskflow.task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request body of {@code POST /projects/{projectId}/tasks}. New tasks always start in {@code TODO}.
 *
 * @param title the task title, 1 to 200 characters
 * @param description optional description, may be null
 * @param priority optional priority; defaults to {@code MEDIUM} when omitted
 * @param dueDate optional due date ({@code yyyy-MM-dd}), may be null
 * @param assigneeId optional assignee user id; must be a project member when present
 */
public record CreateTaskRequest(
    @NotBlank @Size(max = 200) String title,
    String description,
    TaskPriority priority,
    LocalDate dueDate,
    Long assigneeId) {}
