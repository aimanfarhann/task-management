package com.taskflow.dashboard.dto;

import com.taskflow.task.dto.TaskDto;

/** Manual assembly of dashboard DTOs from task and project data (ARCHITECTURE.md §3.1). */
public final class DashboardMapper {

  private DashboardMapper() {}

  /**
   * Combines a task DTO with its owning project's name and color tag into a dashboard task row.
   *
   * @param task the task, whose {@code projectId} identifies the project
   * @param projectName the owning project's name
   * @param projectColorTag the owning project's color tag key
   * @return the contract-shaped dashboard task DTO
   */
  public static DashboardTaskDto toDashboardTask(
      TaskDto task, String projectName, String projectColorTag) {
    return new DashboardTaskDto(
        task.id(),
        task.projectId(),
        task.title(),
        task.description(),
        task.status(),
        task.priority(),
        task.dueDate(),
        task.assignee(),
        task.createdBy(),
        task.createdAt(),
        task.updatedAt(),
        projectName,
        projectColorTag);
  }
}
