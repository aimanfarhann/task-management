package com.taskflow.task;

/**
 * Priority of a task (PRD §5.3). Stored as a string matching the {@code tasks.priority} CHECK
 * constraint (SCHEMA.md §2.4).
 */
public enum TaskPriority {
  /** Low priority. */
  LOW,
  /** Medium priority. The default for a new task when none is supplied. */
  MEDIUM,
  /** High priority. */
  HIGH
}
