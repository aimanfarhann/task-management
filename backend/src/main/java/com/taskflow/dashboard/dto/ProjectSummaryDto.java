package com.taskflow.dashboard.dto;

/**
 * Per-project task rollup on the dashboard (API contract {@code ProjectSummaryDto}).
 *
 * @param projectId the project's database id
 * @param projectName the project name
 * @param colorTag the project's color tag key
 * @param todoCount number of tasks in {@code TODO}
 * @param inProgressCount number of tasks in {@code IN_PROGRESS}
 * @param doneCount number of tasks in {@code DONE}
 */
public record ProjectSummaryDto(
    Long projectId,
    String projectName,
    String colorTag,
    long todoCount,
    long inProgressCount,
    long doneCount) {}
