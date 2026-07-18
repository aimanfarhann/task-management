package com.taskflow.task;

import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.common.security.CurrentUser;
import com.taskflow.task.dto.CreateTaskRequest;
import com.taskflow.task.dto.TaskDto;
import com.taskflow.task.dto.UpdateTaskRequest;
import com.taskflow.task.dto.UpdateTaskStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoints for task CRUD within a project. */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks")
public class TaskController {

  private final TaskService taskService;

  /**
   * Creates the controller.
   *
   * @param taskService the task business logic
   */
  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  /**
   * Lists a project's tasks with optional status/priority/assignee filters. Member or ADMIN only.
   *
   * @param projectId the project id
   * @param status optional status filter
   * @param priority optional priority filter
   * @param assigneeId optional assignee filter
   * @param currentUser the authenticated caller
   * @return 200 with the task list
   */
  @GetMapping
  public List<TaskDto> listTasks(
      @PathVariable Long projectId,
      @RequestParam(required = false) TaskStatus status,
      @RequestParam(required = false) TaskPriority priority,
      @RequestParam(required = false) Long assigneeId,
      @CurrentUser AuthenticatedUser currentUser) {
    return taskService.listTasks(projectId, status, priority, assigneeId, currentUser);
  }

  /**
   * Creates a task in the project. Member or ADMIN only.
   *
   * @param projectId the project id
   * @param request the task data
   * @param currentUser the authenticated caller
   * @return 201 with the created task
   */
  @PostMapping
  public ResponseEntity<TaskDto> createTask(
      @PathVariable Long projectId,
      @Valid @RequestBody CreateTaskRequest request,
      @CurrentUser AuthenticatedUser currentUser) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(taskService.createTask(projectId, request, currentUser));
  }

  /**
   * Returns a single task. Member or ADMIN only.
   *
   * @param projectId the project id
   * @param taskId the task id
   * @param currentUser the authenticated caller
   * @return 200 with the task
   */
  @GetMapping("/{taskId}")
  public TaskDto getTask(
      @PathVariable Long projectId,
      @PathVariable Long taskId,
      @CurrentUser AuthenticatedUser currentUser) {
    return taskService.getTask(projectId, taskId, currentUser);
  }

  /**
   * Replaces a task's mutable fields. Member or ADMIN only.
   *
   * @param projectId the project id
   * @param taskId the task id
   * @param request the replacement data
   * @param currentUser the authenticated caller
   * @return 200 with the updated task
   */
  @PutMapping("/{taskId}")
  public TaskDto updateTask(
      @PathVariable Long projectId,
      @PathVariable Long taskId,
      @Valid @RequestBody UpdateTaskRequest request,
      @CurrentUser AuthenticatedUser currentUser) {
    return taskService.updateTask(projectId, taskId, request, currentUser);
  }

  /**
   * Changes only a task's status (board drag-and-drop). Member or ADMIN only.
   *
   * @param projectId the project id
   * @param taskId the task id
   * @param request the new status
   * @param currentUser the authenticated caller
   * @return 200 with the updated task
   */
  @PatchMapping("/{taskId}/status")
  public TaskDto updateStatus(
      @PathVariable Long projectId,
      @PathVariable Long taskId,
      @Valid @RequestBody UpdateTaskStatusRequest request,
      @CurrentUser AuthenticatedUser currentUser) {
    return taskService.updateStatus(projectId, taskId, request, currentUser);
  }

  /**
   * Deletes a task. Member or ADMIN only.
   *
   * @param projectId the project id
   * @param taskId the task id
   * @param currentUser the authenticated caller
   * @return 204 with no body
   */
  @DeleteMapping("/{taskId}")
  public ResponseEntity<Void> deleteTask(
      @PathVariable Long projectId,
      @PathVariable Long taskId,
      @CurrentUser AuthenticatedUser currentUser) {
    taskService.deleteTask(projectId, taskId, currentUser);
    return ResponseEntity.noContent().build();
  }
}
