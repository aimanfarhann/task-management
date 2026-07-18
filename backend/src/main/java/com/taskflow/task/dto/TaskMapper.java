package com.taskflow.task.dto;

import com.taskflow.task.Task;
import com.taskflow.task.TaskComment;
import com.taskflow.user.User;

/** Manual entity-to-DTO mapping for tasks and comments (ARCHITECTURE.md §3.1 — no MapStruct). */
public final class TaskMapper {

  private TaskMapper() {}

  /**
   * Maps a task entity to its API representation. The assignee and creator associations must
   * already be loaded (join-fetched or freshly set).
   *
   * @param task the persisted task entity
   * @return the contract-shaped DTO
   */
  public static TaskDto toDto(Task task) {
    return new TaskDto(
        task.getId(),
        task.getProject().getId(),
        task.getTitle(),
        task.getDescription(),
        task.getStatus(),
        task.getPriority(),
        task.getDueDate(),
        toUserSummary(task.getAssignee()),
        toUserSummary(task.getCreatedBy()),
        task.getCreatedAt(),
        task.getUpdatedAt());
  }

  /**
   * Maps a comment entity to its API representation. The author association must already be loaded.
   *
   * @param comment the persisted comment entity
   * @return the contract-shaped DTO
   */
  public static CommentDto toCommentDto(TaskComment comment) {
    return new CommentDto(
        comment.getId(),
        comment.getTask().getId(),
        toUserSummary(comment.getAuthor()),
        comment.getBody(),
        comment.getCreatedAt());
  }

  /**
   * Maps a user entity to the minimal summary embedded in task and comment payloads.
   *
   * @param user the user entity, or null
   * @return the summary, or null when the user is null
   */
  public static UserSummary toUserSummary(User user) {
    return user == null ? null : new UserSummary(user.getId(), user.getDisplayName());
  }
}
