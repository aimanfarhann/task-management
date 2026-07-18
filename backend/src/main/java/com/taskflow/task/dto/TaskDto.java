package com.taskflow.task.dto;

import com.taskflow.task.TaskPriority;
import com.taskflow.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * API representation of a task (API contract {@code TaskDto}).
 *
 * @param id the task's database id
 * @param projectId the owning project's id
 * @param title the task title
 * @param description the description, or null
 * @param status the workflow status
 * @param priority the priority
 * @param dueDate the due date as {@code yyyy-MM-dd}, or null
 * @param assignee the assigned user, or null when unassigned
 * @param createdBy the user who created the task
 * @param createdAt creation timestamp, serialized as an ISO-8601 string
 * @param updatedAt last-update timestamp, serialized as an ISO-8601 string
 */
public record TaskDto(
    Long id,
    Long projectId,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    LocalDate dueDate,
    UserSummary assignee,
    UserSummary createdBy,
    Instant createdAt,
    Instant updatedAt) {}
