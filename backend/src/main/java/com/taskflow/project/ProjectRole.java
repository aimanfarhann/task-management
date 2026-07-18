package com.taskflow.project;

/**
 * Role a user holds within a project (PRD §4). Stored as a string matching the CHECK constraint.
 */
public enum ProjectRole {
  /** Can edit/archive the project and manage members, plus all MEMBER abilities. */
  OWNER,
  /** Can work with tasks and comments inside the project. */
  MEMBER
}
