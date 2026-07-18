package com.taskflow.dashboard.dto;

import com.taskflow.task.TaskPriority;
import com.taskflow.task.TaskStatus;
import com.taskflow.task.dto.UserSummary;
import java.time.Instant;
import java.time.LocalDate;

/**
 * API representation of a task on the dashboard: all {@code TaskDto} fields plus the owning
 * project's name and color tag so the SPA can render it without a second lookup (API contract
 * {@code DashboardTaskDto}).
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
 * @param projectName the owning project's name
 * @param projectColorTag the owning project's color tag key
 */
public record DashboardTaskDto(
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
    Instant updatedAt,
    String projectName,
    String projectColorTag) {}
