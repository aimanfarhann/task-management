package com.taskflow.task;

import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.project.ProjectAuthService;
import com.taskflow.task.dto.CommentDto;
import com.taskflow.task.dto.CreateCommentRequest;
import com.taskflow.task.dto.TaskMapper;
import com.taskflow.user.User;
import com.taskflow.user.UserService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task comment business logic: listing, creating, and deleting comments. Every method asserts
 * project membership first (SCHEMA.md §4) and confirms the task belongs to the path project before
 * acting.
 */
@Service
public class TaskCommentService {

  private static final Logger log = LoggerFactory.getLogger(TaskCommentService.class);

  private final TaskCommentRepository taskCommentRepository;
  private final TaskService taskService;
  private final ProjectAuthService projectAuthService;
  private final UserService userService;

  /**
   * Creates the service.
   *
   * @param taskCommentRepository data access for comments
   * @param taskService resolves the task and enforces the cross-project 404 guard
   * @param projectAuthService project-scoped authorization checks
   * @param userService resolves the comment author
   */
  public TaskCommentService(
      TaskCommentRepository taskCommentRepository,
      TaskService taskService,
      ProjectAuthService projectAuthService,
      UserService userService) {
    this.taskCommentRepository = taskCommentRepository;
    this.taskService = taskService;
    this.projectAuthService = projectAuthService;
    this.userService = userService;
  }

  /**
   * Lists a task's comments in chronological order. Member or ADMIN only.
   *
   * @param projectId the project id
   * @param taskId the task id
   * @param currentUser the authenticated caller
   * @return the comments oldest first
   */
  @Transactional(readOnly = true)
  public List<CommentDto> listComments(Long projectId, Long taskId, AuthenticatedUser currentUser) {
    projectAuthService.assertMember(projectId, currentUser);
    taskService.requireTask(projectId, taskId);
    return taskCommentRepository.findByTaskIdWithAuthor(taskId).stream()
        .map(TaskMapper::toCommentDto)
        .toList();
  }

  /**
   * Adds a comment to a task. Member or ADMIN only. The author is the caller.
   *
   * @param projectId the project id
   * @param taskId the task id
   * @param request the validated comment data
   * @param currentUser the authenticated caller
   * @return the created comment
   */
  @Transactional
  public CommentDto createComment(
      Long projectId, Long taskId, CreateCommentRequest request, AuthenticatedUser currentUser) {
    projectAuthService.assertMember(projectId, currentUser);
    Task task = taskService.requireTask(projectId, taskId);
    User author =
        userService
            .findById(currentUser.id())
            .orElseThrow(
                () -> new IllegalStateException("User " + currentUser.id() + " not found"));
    TaskComment comment = taskCommentRepository.save(new TaskComment(task, author, request.body()));
    log.info("Comment {} created on task {} by user {}", comment.getId(), taskId, currentUser.id());
    return TaskMapper.toCommentDto(comment);
  }

  /**
   * Deletes a comment. The comment author may delete their own comment; a project OWNER or system
   * ADMIN may delete any comment. Anyone else is forbidden.
   *
   * @param projectId the project id
   * @param taskId the task id
   * @param commentId the comment id
   * @param currentUser the authenticated caller
   * @throws ResourceNotFoundException with code {@code COMMENT_NOT_FOUND} if the comment does not
   *     exist or belongs to another task
   */
  @Transactional
  public void deleteComment(
      Long projectId, Long taskId, Long commentId, AuthenticatedUser currentUser) {
    projectAuthService.assertMember(projectId, currentUser);
    taskService.requireTask(projectId, taskId);
    TaskComment comment =
        taskCommentRepository
            .findByIdWithAuthor(commentId)
            .filter(candidate -> candidate.getTask().getId().equals(taskId))
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "COMMENT_NOT_FOUND", "Comment " + commentId + " was not found"));
    if (!comment.getAuthor().getId().equals(currentUser.id())) {
      // Not the author — only a project OWNER (or ADMIN) may delete someone else's comment.
      projectAuthService.assertOwner(projectId, currentUser);
    }
    taskCommentRepository.delete(comment);
    log.info("Comment {} deleted from task {} by user {}", commentId, taskId, currentUser.id());
  }
}
