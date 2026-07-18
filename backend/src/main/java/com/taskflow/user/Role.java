package com.taskflow.user;

/** System-wide user role (PRD §4). Stored as a string matching the DB CHECK constraint. */
public enum Role {
  /** Default role: can create and join projects and manage tasks within them. */
  USER,
  /** System administrator: all USER abilities plus visibility into every project. */
  ADMIN
}
