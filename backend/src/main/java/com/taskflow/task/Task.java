package com.taskflow.task;

import com.taskflow.project.Project;
import com.taskflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** JPA entity mirroring the {@code tasks} table (SCHEMA.md §2.4). */
@Entity
@Table(name = "tasks")
public class Task {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id")
  private Project project;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TaskStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TaskPriority priority;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id")
  private User assignee;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** JPA-only constructor. */
  protected Task() {}

  /**
   * Creates a new task in {@link TaskStatus#TODO}. Creation timestamps are set to now.
   *
   * @param project the owning project
   * @param title the task title, 1 to 200 characters
   * @param description optional free-text description, may be null
   * @param priority the task priority
   * @param dueDate optional due date, may be null
   * @param assignee the assigned user, or null when unassigned
   * @param createdBy the user who created the task
   */
  public Task(
      Project project,
      String title,
      String description,
      TaskPriority priority,
      LocalDate dueDate,
      User assignee,
      User createdBy) {
    this.project = project;
    this.title = title;
    this.description = description;
    this.status = TaskStatus.TODO;
    this.priority = priority;
    this.dueDate = dueDate;
    this.assignee = assignee;
    this.createdBy = createdBy;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  /**
   * Applies a full update from {@code PUT /tasks/{id}} — every mutable field is replaced.
   *
   * @param title the new title
   * @param description the new description, may be null
   * @param status the new workflow status
   * @param priority the new priority
   * @param dueDate the new due date, may be null
   * @param assignee the new assignee, or null to unassign
   */
  public void applyUpdate(
      String title,
      String description,
      TaskStatus status,
      TaskPriority priority,
      LocalDate dueDate,
      User assignee) {
    this.title = title;
    this.description = description;
    this.status = status;
    this.priority = priority;
    this.dueDate = dueDate;
    this.assignee = assignee;
  }

  /**
   * Moves the task to a new status (board drag-and-drop). Any transition is allowed.
   *
   * @param status the new workflow status
   */
  public void changeStatus(TaskStatus status) {
    this.status = status;
  }

  /** Refreshes {@code updated_at} on every persisted mutation. */
  @PreUpdate
  void touchUpdatedAt() {
    this.updatedAt = Instant.now();
  }

  /** Returns the database id, or null if not yet persisted. */
  public Long getId() {
    return id;
  }

  /** Returns the owning project. */
  public Project getProject() {
    return project;
  }

  /** Returns the task title. */
  public String getTitle() {
    return title;
  }

  /** Returns the description, or null if none was provided. */
  public String getDescription() {
    return description;
  }

  /** Returns the workflow status. */
  public TaskStatus getStatus() {
    return status;
  }

  /** Returns the priority. */
  public TaskPriority getPriority() {
    return priority;
  }

  /** Returns the due date, or null if none was set. */
  public LocalDate getDueDate() {
    return dueDate;
  }

  /** Returns the assigned user, or null if unassigned. */
  public User getAssignee() {
    return assignee;
  }

  /** Returns the user who created the task. */
  public User getCreatedBy() {
    return createdBy;
  }

  /** Returns the creation timestamp. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns the timestamp of the last update. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
