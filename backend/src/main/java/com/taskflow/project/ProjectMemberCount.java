package com.taskflow.project;

/** Repository projection pairing a project id with its member count. */
public interface ProjectMemberCount {

  /** Returns the project id. */
  Long getProjectId();

  /** Returns the number of members in that project. */
  long getMemberCount();
}
