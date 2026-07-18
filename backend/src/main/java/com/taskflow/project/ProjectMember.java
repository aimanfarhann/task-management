package com.taskflow.project;

import com.taskflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;

/** JPA entity mirroring the {@code project_members} join table (SCHEMA.md §2.3). */
@Entity
@Table(name = "project_members")
public class ProjectMember {

  @EmbeddedId private ProjectMemberId id;

  @MapsId("projectId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id")
  private Project project;

  @MapsId("userId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "project_role", nullable = false, length = 20)
  private ProjectRole projectRole;

  @Column(name = "joined_at", nullable = false)
  private Instant joinedAt;

  /** JPA-only constructor. */
  protected ProjectMember() {}

  /**
   * Creates a membership joining a user to a project.
   *
   * @param project the project joined
   * @param user the joining user
   * @param projectRole the role the user holds within the project
   */
  public ProjectMember(Project project, User user, ProjectRole projectRole) {
    this.id = new ProjectMemberId(project.getId(), user.getId());
    this.project = project;
    this.user = user;
    this.projectRole = projectRole;
    this.joinedAt = Instant.now();
  }

  /** Returns the composite key. */
  public ProjectMemberId getId() {
    return id;
  }

  /** Returns the project. */
  public Project getProject() {
    return project;
  }

  /** Returns the member user. */
  public User getUser() {
    return user;
  }

  /** Returns the role the user holds within the project. */
  public ProjectRole getProjectRole() {
    return projectRole;
  }

  /** Returns when the user joined the project. */
  public Instant getJoinedAt() {
    return joinedAt;
  }
}
