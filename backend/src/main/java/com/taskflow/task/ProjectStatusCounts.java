package com.taskflow.task;

/**
 * Task counts for a single project, one field per {@link TaskStatus}. Returned by {@link
 * TaskService} to the dashboard feature, keyed by project id.
 *
 * @param todoCount number of {@link TaskStatus#TODO} tasks
 * @param inProgressCount number of {@link TaskStatus#IN_PROGRESS} tasks
 * @param doneCount number of {@link TaskStatus#DONE} tasks
 */
public record ProjectStatusCounts(long todoCount, long inProgressCount, long doneCount) {

  /** All-zero counts, used for a project that has no tasks. */
  public static final ProjectStatusCounts ZERO = new ProjectStatusCounts(0, 0, 0);
}
