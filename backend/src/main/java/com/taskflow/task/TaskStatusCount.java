package com.taskflow.task;

/** Repository projection pairing a project id and task status with the count of tasks in it. */
public interface TaskStatusCount {

  /** Returns the project id. */
  Long getProjectId();

  /** Returns the task status. */
  TaskStatus getStatus();

  /** Returns the number of tasks in that project with that status. */
  long getCount();
}
