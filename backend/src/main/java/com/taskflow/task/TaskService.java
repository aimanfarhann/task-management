package com.taskflow.task;

import com.taskflow.common.exception.BadRequestException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectAuthService;
import com.taskflow.project.ProjectService;
import com.taskflow.task.dto.CreateTaskRequest;
import com.taskflow.task.dto.TaskDto;
import com.taskflow.task.dto.TaskMapper;
import com.taskflow.task.dto.UpdateTaskRequest;
import com.taskflow.task.dto.UpdateTaskStatusRequest;
import com.taskflow.user.User;
import com.taskflow.user.UserService;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task CRUD business logic. Every project-scoped method asserts membership as its first line
 * (SCHEMA.md §4), then guards that the task belongs to the path project before acting, so one
 * project's tasks are never reachable through another's route.
 */
@Service
public class TaskService {

  private static final Logger log = LoggerFactory.getLogger(TaskService.class);

  private final TaskRepository taskRepository;
  private final ProjectAuthService projectAuthService;
  private final ProjectService projectService;
  private final UserService userService;

  /**
   * Creates the service.
   *
   * @param taskRepository data access for tasks
   * @param projectAuthService project-scoped authorization checks and membership lookups
   * @param projectService resolves the owning project when creating a task
   * @param userService resolves the creator and assignee users
   */
  public TaskService(
      TaskRepository taskRepository,
      ProjectAuthService projectAuthService,
      ProjectService projectService,
      UserService userService) {
    this.taskRepository = taskRepository;
    this.projectAuthService = projectAuthService;
    this.projectService = projectService;
    this.userService = userService;
  }

  /**
   * Lists a project's tasks with optional filters. Member or ADMIN only.
   *
   * @param projectId the project id
   * @param status optional status filter, or null for all
   * @param priority optional priority filter, or null for all
   * @param assigneeId optional assignee filter, or null for any
   * @param currentUser the authenticated caller
   * @return the matching tasks, oldest first
   */
  @Transactional(readOnly = true)
  public List<TaskDto> listTasks(
      Long projectId,
      TaskStatus status,
      TaskPriority priority,
      Long assigneeId,
      AuthenticatedUser currentUser) {
    projectAuthService.assertMember(projectId, currentUser);
    return taskRepository.findByProjectFiltered(projectId, status, priority, assigneeId).stream()
        .map(TaskMapper::toDto)
        .toList();
  }

  /**
   * Creates a task in a project. Member or ADMIN only. The creator is the caller.
   *
   * @param projectId the project id
   * @param request the validated task data
   * @param currentUser the authenticated caller
   * @return the created task
   * @throws BadRequestException with code {@code ASSIGNEE_NOT_MEMBER} if the assignee is not a
   *     project member
   */
  @Transactional
  public TaskDto createTask(
      Long projectId, CreateTaskRequest request, AuthenticatedUser currentUser) {
    projectAuthService.assertMember(projectId, currentUser);
    User assignee = resolveAssignee(projectId, request.assigneeId());
    Project project =
        projectService
            .findById(projectId)
            .orElseThrow(
                () -> new IllegalStateException("Project " + projectId + " vanished mid-request"));
    User creator = loadUser(currentUser.id());
    TaskPriority priority = request.priority() == null ? TaskPriority.MEDIUM : request.priority();
    Task task =
        taskRepository.save(
            new Task(
                project,
                request.title(),
                request.description(),
                priority,
                request.dueDate(),
                assignee,
                creator));
    log.info("Task {} created in project {} by user {}", task.getId(), projectId, currentUser.id());
    return TaskMapper.toDto(task);
  }

  /**
   * Returns a single task. Member or ADMIN only.
   *
   * @param projectId the project id
   * @param taskId the task id
   * @param currentUser the authenticated caller
   * @return the task
   * @throws ResourceNotFoundException with code {@code TASK_NOT_FOUND} if the task does not exist
   *     or belongs to another project
   */
  @Transactional(readOnly = true)
  public TaskDto getTask(Long projectId, Long taskId, AuthenticatedUser currentUser) {
    projectAuthService.assertMember(projectId, currentUser);
    return TaskMapper.toDto(requireTask(projectId, taskId));
  }

  /**
   * Replaces a task's mutable fields. Member or ADMIN only.
   *
   * @param projectId the project id
   * @param taskId the task id
   * @param request the validated replacement data
   * @param currentUser the authenticated caller
   * @return the updated task
   * @throws ResourceNotFoundException with code {@code TASK_NOT_FOUND} if the task does not exist
   *     or belongs to another project
   * @throws BadRequestException with code {@code ASSIGNEE_NOT_MEMBER} if the assignee is not a
   *     project member
   */
  @Transactional
  public TaskDto updateTask(
      Long projectId, Long taskId, UpdateTaskRequest request, AuthenticatedUser currentUser) {
    projectAuthService.assertMember(projectId, currentUser);
    Task task = requireTask(projectId, taskId);
    User assignee = resolveAssignee(projectId, request.assigneeId());
    task.applyUpdate(
        request.title(),
        request.description(),
        request.status(),
        request.priority(),
        request.dueDate(),
        assignee);
    Task saved = taskRepository.saveAndFlush(task);
    log.info("Task {} updated in project {} by user {}", taskId, projectId, currentUser.id());
    return TaskMapper.toDto(saved);
  }

  /**
   * Changes only a task's status (board drag-and-drop). Member or ADMIN only.
   *
   * @param projectId the project id
   * @param taskId the task id
   * @param request the new status
   * @param currentUser the authenticated caller
   * @return the updated task
   * @throws ResourceNotFoundException with code {@code TASK_NOT_FOUND} if the task does not exist
   *     or belongs to another project
   */
  @Transactional
  public TaskDto updateStatus(
      Long projectId, Long taskId, UpdateTaskStatusRequest request, AuthenticatedUser currentUser) {
    projectAuthService.assertMember(projectId, currentUser);
    Task task = requireTask(projectId, taskId);
    task.changeStatus(request.status());
    Task saved = taskRepository.saveAndFlush(task);
    log.info(
        "Task {} status changed to {} in project {} by user {}",
        taskId,
        request.status(),
        projectId,
        currentUser.id());
    return TaskMapper.toDto(saved);
  }

  /**
   * Deletes a task. Member or ADMIN only.
   *
   * @param projectId the project id
   * @param taskId the task id
   * @param currentUser the authenticated caller
   * @throws ResourceNotFoundException with code {@code TASK_NOT_FOUND} if the task does not exist
   *     or belongs to another project
   */
  @Transactional
  public void deleteTask(Long projectId, Long taskId, AuthenticatedUser currentUser) {
    projectAuthService.assertMember(projectId, currentUser);
    Task task = requireTask(projectId, taskId);
    taskRepository.delete(task);
    log.info("Task {} deleted from project {} by user {}", taskId, projectId, currentUser.id());
  }

  /**
   * Returns the tasks assigned to a user across the given projects, for the dashboard.
   *
   * @param userId the assignee's user id
   * @param projectIds the projects to search within
   * @return the user's assigned tasks; empty when no projects are given
   */
  @Transactional(readOnly = true)
  public List<TaskDto> listAssignedTasks(Long userId, Collection<Long> projectIds) {
    if (projectIds.isEmpty()) {
      return List.of();
    }
    return taskRepository.findAssignedInProjects(userId, projectIds).stream()
        .map(TaskMapper::toDto)
        .toList();
  }

  /**
   * Returns task counts by status for each of the given projects, for the dashboard summaries.
   * Projects with no tasks are absent from the result — callers substitute {@link
   * ProjectStatusCounts#ZERO}.
   *
   * @param projectIds the projects to count within
   * @return counts keyed by project id
   */
  @Transactional(readOnly = true)
  public Map<Long, ProjectStatusCounts> statusCountsByProject(Collection<Long> projectIds) {
    if (projectIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, EnumMap<TaskStatus, Long>> byProject = new HashMap<>();
    for (TaskStatusCount row : taskRepository.countByStatusInProjects(projectIds)) {
      byProject
          .computeIfAbsent(row.getProjectId(), key -> new EnumMap<>(TaskStatus.class))
          .put(row.getStatus(), row.getCount());
    }
    Map<Long, ProjectStatusCounts> result = new HashMap<>();
    byProject.forEach(
        (projectId, counts) ->
            result.put(
                projectId,
                new ProjectStatusCounts(
                    counts.getOrDefault(TaskStatus.TODO, 0L),
                    counts.getOrDefault(TaskStatus.IN_PROGRESS, 0L),
                    counts.getOrDefault(TaskStatus.DONE, 0L))));
    return result;
  }

  /**
   * Loads a task, enforcing that it belongs to the given project (the cross-project 404 guard).
   * Returns the managed entity so the comment service in this feature can set it as a new comment's
   * FK target; callers assert membership first. Mirrors {@link
   * com.taskflow.user.UserService#findById(Long)} in returning an entity for association use.
   *
   * @param projectId the project the task must belong to
   * @param taskId the task id
   * @return the managed task entity
   * @throws ResourceNotFoundException with code {@code TASK_NOT_FOUND} if the task does not exist
   *     or belongs to another project
   */
  @Transactional(readOnly = true)
  public Task requireTask(Long projectId, Long taskId) {
    return taskRepository
        .findByIdWithDetails(taskId)
        .filter(task -> task.getProject().getId().equals(projectId))
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "TASK_NOT_FOUND", "Task " + taskId + " was not found"));
  }

  private User resolveAssignee(Long projectId, Long assigneeId) {
    if (assigneeId == null) {
      return null;
    }
    if (!projectAuthService.isMember(projectId, assigneeId)) {
      throw new BadRequestException(
          "ASSIGNEE_NOT_MEMBER", "Assignee " + assigneeId + " is not a member of this project");
    }
    return loadUser(assigneeId);
  }

  private User loadUser(Long userId) {
    return userService
        .findById(userId)
        .orElseThrow(() -> new IllegalStateException("User " + userId + " not found"));
  }
}
