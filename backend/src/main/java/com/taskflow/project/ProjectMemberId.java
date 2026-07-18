package com.taskflow.project;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Composite primary key of {@code project_members}: (project_id, user_id). */
@Embeddable
public class ProjectMemberId implements Serializable {

  private static final long serialVersionUID = 1L;

  @Column(name = "project_id")
  private Long projectId;

  @Column(name = "user_id")
  private Long userId;

  /** JPA-only constructor. */
  protected ProjectMemberId() {}

  /**
   * Creates the composite key.
   *
   * @param projectId the project's database id
   * @param userId the user's database id
   */
  public ProjectMemberId(Long projectId, Long userId) {
    this.projectId = projectId;
    this.userId = userId;
  }

  /** Returns the project id component. */
  public Long getProjectId() {
    return projectId;
  }

  /** Returns the user id component. */
  public Long getUserId() {
    return userId;
  }

  /** Composite keys must implement value equality for JPA identity handling. */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ProjectMemberId that)) {
      return false;
    }
    return Objects.equals(projectId, that.projectId) && Objects.equals(userId, that.userId);
  }

  /** Hash consistent with {@link #equals(Object)}. */
  @Override
  public int hashCode() {
    return Objects.hash(projectId, userId);
  }
}
