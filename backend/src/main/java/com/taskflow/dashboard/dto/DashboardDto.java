package com.taskflow.dashboard.dto;

import java.util.List;

/**
 * The authenticated caller's dashboard (API contract {@code DashboardDto}).
 *
 * @param myTasks tasks assigned to the caller across every project they belong to
 * @param projectSummaries one rollup row per project the caller belongs to
 */
public record DashboardDto(
    List<DashboardTaskDto> myTasks, List<ProjectSummaryDto> projectSummaries) {}
