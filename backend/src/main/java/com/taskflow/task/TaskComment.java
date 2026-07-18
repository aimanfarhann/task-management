package com.taskflow.task;

import com.taskflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** JPA entity mirroring the {@code task_comments} table (SCHEMA.md §2.5). */
@Entity
@Table(name = "task_comments")
public class TaskComment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "task_id")
  private Task task;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id")
  private User author;

  @Column(columnDefinition = "text", nullable = false)
  private String body;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** JPA-only constructor. */
  protected TaskComment() {}

  /**
   * Creates a comment on a task.
   *
   * @param task the commented task
   * @param author the user writing the comment
   * @param body the comment text, 1 to 2000 characters
   */
  public TaskComment(Task task, User author, String body) {
    this.task = task;
    this.author = author;
    this.body = body;
    this.createdAt = Instant.now();
  }

  /** Returns the database id, or null if not yet persisted. */
  public Long getId() {
    return id;
  }

  /** Returns the commented task. */
  public Task getTask() {
    return task;
  }

  /** Returns the comment author. */
  public User getAuthor() {
    return author;
  }

  /** Returns the comment text. */
  public String getBody() {
    return body;
  }

  /** Returns the creation timestamp. */
  public Instant getCreatedAt() {
    return createdAt;
  }
}
