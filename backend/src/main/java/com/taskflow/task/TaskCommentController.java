package com.taskflow.task;

import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.common.security.CurrentUser;
import com.taskflow.task.dto.CommentDto;
import com.taskflow.task.dto.CreateCommentRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoints for comments on a task within a project. */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks/{taskId}/comments")
public class TaskCommentController {

  private final TaskCommentService taskCommentService;

  /**
   * Creates the controller.
   *
   * @param taskCommentService the comment business logic
   */
  public TaskCommentController(TaskCommentService taskCommentService) {
    this.taskCommentService = taskCommentService;
  }

  /**
   * Lists a task's comments in chronological order. Member or ADMIN only.
   *
   * @param projectId the project id
   * @param taskId the task id
   * @param currentUser the authenticated caller
   * @return 200 with the comment list
   */
  @GetMapping
  public List<CommentDto> listComments(
      @PathVariable Long projectId,
      @PathVariable Long taskId,
      @CurrentUser AuthenticatedUser currentUser) {
    return taskCommentService.listComments(projectId, taskId, currentUser);
  }

  /**
   * Adds a comment to the task. Member or ADMIN only.
   *
   * @param projectId the project id
   * @param taskId the task id
   * @param request the comment data
   * @param currentUser the authenticated caller
   * @return 201 with the created comment
   */
  @PostMapping
  public ResponseEntity<CommentDto> createComment(
      @PathVariable Long projectId,
      @PathVariable Long taskId,
      @Valid @RequestBody CreateCommentRequest request,
      @CurrentUser AuthenticatedUser currentUser) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(taskCommentService.createComment(projectId, taskId, request, currentUser));
  }

  /**
   * Deletes a comment. The author, or a project OWNER/ADMIN, may delete.
   *
   * @param projectId the project id
   * @param taskId the task id
   * @param commentId the comment id
   * @param currentUser the authenticated caller
   * @return 204 with no body
   */
  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @PathVariable Long projectId,
      @PathVariable Long taskId,
      @PathVariable Long commentId,
      @CurrentUser AuthenticatedUser currentUser) {
    taskCommentService.deleteComment(projectId, taskId, commentId, currentUser);
    return ResponseEntity.noContent().build();
  }
}
