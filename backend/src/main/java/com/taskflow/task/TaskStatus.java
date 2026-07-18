package com.taskflow.task;

/**
 * Workflow status of a task (PRD §5.3). Stored as a string matching the {@code tasks.status} CHECK
 * constraint (SCHEMA.md §2.4). Any transition between statuses is allowed.
 */
public enum TaskStatus {
  /** Not yet started. New tasks begin here. */
  TODO,
  /** Actively being worked on. */
  IN_PROGRESS,
  /** Completed. */
  DONE
}
