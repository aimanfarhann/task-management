package com.taskflow.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.taskflow.common.exception.BadRequestException;
import com.taskflow.common.exception.ForbiddenException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectAuthService;
import com.taskflow.project.ProjectService;
import com.taskflow.task.dto.CreateTaskRequest;
import com.taskflow.task.dto.TaskDto;
import com.taskflow.task.dto.UpdateTaskRequest;
import com.taskflow.task.dto.UpdateTaskStatusRequest;
import com.taskflow.testutil.TestData;
import com.taskflow.user.User;
import com.taskflow.user.UserService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for {@link TaskService} business rules. */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  private static final Long PROJECT_ID = 1L;
  private static final Long TASK_ID = 5L;
  private static final Long CALLER_ID = 7L;
  private static final Long ASSIGNEE_ID = 8L;

  @Mock private TaskRepository taskRepository;
  @Mock private ProjectAuthService projectAuthService;
  @Mock private ProjectService projectService;
  @Mock private UserService userService;

  @InjectMocks private TaskService taskService;

  @Test
  void createTask_valid_assertsMembershipThenSavesWithDefaults() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    Project project = TestData.project(PROJECT_ID, "Project");
    User creator = TestData.user(CALLER_ID, "creator@test.com");
    when(projectService.findById(PROJECT_ID)).thenReturn(Optional.of(project));
    when(userService.findById(CALLER_ID)).thenReturn(Optional.of(creator));
    when(taskRepository.save(any(Task.class)))
        .thenAnswer(
            invocation -> {
              Task saved = invocation.getArgument(0);
              ReflectionTestUtils.setField(saved, "id", 100L);
              return saved;
            });

    // Act
    TaskDto dto =
        taskService.createTask(
            PROJECT_ID,
            new CreateTaskRequest(
                "Write tests", "Cover the rules", null, LocalDate.of(2026, 1, 2), null),
            caller);

    // Assert
    assertThat(dto.id()).isEqualTo(100L);
    assertThat(dto.projectId()).isEqualTo(PROJECT_ID);
    assertThat(dto.title()).isEqualTo("Write tests");
    assertThat(dto.status()).isEqualTo(TaskStatus.TODO);
    assertThat(dto.priority()).isEqualTo(TaskPriority.MEDIUM);
    assertThat(dto.dueDate()).isEqualTo(LocalDate.of(2026, 1, 2));
    assertThat(dto.assignee()).isNull();
    assertThat(dto.createdBy().userId()).isEqualTo(CALLER_ID);
    InOrder order = inOrder(projectAuthService, taskRepository);
    order.verify(projectAuthService).assertMember(PROJECT_ID, caller);
    order.verify(taskRepository).save(any(Task.class));
  }

  @Test
  void createTask_assigneeIsMember_setsAssignee() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    Project project = TestData.project(PROJECT_ID, "Project");
    User creator = TestData.user(CALLER_ID, "creator@test.com");
    User assignee = TestData.user(ASSIGNEE_ID, "assignee@test.com");
    when(projectAuthService.isMember(PROJECT_ID, ASSIGNEE_ID)).thenReturn(true);
    when(userService.findById(ASSIGNEE_ID)).thenReturn(Optional.of(assignee));
    when(projectService.findById(PROJECT_ID)).thenReturn(Optional.of(project));
    when(userService.findById(CALLER_ID)).thenReturn(Optional.of(creator));
    when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    TaskDto dto =
        taskService.createTask(
            PROJECT_ID,
            new CreateTaskRequest("Assigned", null, TaskPriority.HIGH, null, ASSIGNEE_ID),
            caller);

    // Assert
    assertThat(dto.assignee()).isNotNull();
    assertThat(dto.assignee().userId()).isEqualTo(ASSIGNEE_ID);
    assertThat(dto.priority()).isEqualTo(TaskPriority.HIGH);
  }

  @Test
  void createTask_assigneeNotMember_throwsBadRequestWithoutSaving() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    when(projectAuthService.isMember(PROJECT_ID, ASSIGNEE_ID)).thenReturn(false);

    // Act + Assert
    assertThatThrownBy(
            () ->
                taskService.createTask(
                    PROJECT_ID,
                    new CreateTaskRequest("Bad assignee", null, null, null, ASSIGNEE_ID),
                    caller))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("ASSIGNEE_NOT_MEMBER"));
    verify(taskRepository, never()).save(any());
  }

  @Test
  void createTask_nonMember_propagatesForbiddenWithoutTouchingRepository() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    doThrow(new ForbiddenException("not a member"))
        .when(projectAuthService)
        .assertMember(PROJECT_ID, caller);

    // Act + Assert
    assertThatThrownBy(
            () ->
                taskService.createTask(
                    PROJECT_ID, new CreateTaskRequest("Blocked", null, null, null, null), caller))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(taskRepository);
  }

  @Test
  void getTask_member_returnsDto() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    Task task = taskInProject(PROJECT_ID);
    when(taskRepository.findByIdWithDetails(TASK_ID)).thenReturn(Optional.of(task));

    // Act
    TaskDto dto = taskService.getTask(PROJECT_ID, TASK_ID, caller);

    // Assert
    assertThat(dto.id()).isEqualTo(TASK_ID);
    InOrder order = inOrder(projectAuthService, taskRepository);
    order.verify(projectAuthService).assertMember(PROJECT_ID, caller);
    order.verify(taskRepository).findByIdWithDetails(TASK_ID);
  }

  @Test
  void getTask_taskBelongsToAnotherProject_throwsTaskNotFound() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    Task task = taskInProject(999L);
    when(taskRepository.findByIdWithDetails(TASK_ID)).thenReturn(Optional.of(task));

    // Act + Assert
    assertThatThrownBy(() -> taskService.getTask(PROJECT_ID, TASK_ID, caller))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("TASK_NOT_FOUND"));
  }

  @Test
  void getTask_unknownTask_throwsTaskNotFound() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    when(taskRepository.findByIdWithDetails(TASK_ID)).thenReturn(Optional.empty());

    // Act + Assert
    assertThatThrownBy(() -> taskService.getTask(PROJECT_ID, TASK_ID, caller))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("TASK_NOT_FOUND"));
  }

  @Test
  void updateTask_valid_replacesFieldsAndUnassigns() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    Task task = taskInProject(PROJECT_ID);
    when(taskRepository.findByIdWithDetails(TASK_ID)).thenReturn(Optional.of(task));
    when(taskRepository.saveAndFlush(task)).thenReturn(task);

    // Act
    TaskDto dto =
        taskService.updateTask(
            PROJECT_ID,
            TASK_ID,
            new UpdateTaskRequest(
                "Renamed", "New body", TaskStatus.IN_PROGRESS, TaskPriority.LOW, null, null),
            caller);

    // Assert
    assertThat(dto.title()).isEqualTo("Renamed");
    assertThat(dto.description()).isEqualTo("New body");
    assertThat(dto.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    assertThat(dto.priority()).isEqualTo(TaskPriority.LOW);
    assertThat(dto.assignee()).isNull();
    assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  void updateTask_assigneeNotMember_throwsBadRequest() {
    // Arrange: the task exists in the project, but the requested assignee is not a member.
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    Task task = taskInProject(PROJECT_ID);
    when(taskRepository.findByIdWithDetails(TASK_ID)).thenReturn(Optional.of(task));
    when(projectAuthService.isMember(PROJECT_ID, ASSIGNEE_ID)).thenReturn(false);

    // Act + Assert
    assertThatThrownBy(
            () ->
                taskService.updateTask(
                    PROJECT_ID,
                    TASK_ID,
                    new UpdateTaskRequest(
                        "Title", null, TaskStatus.TODO, TaskPriority.MEDIUM, null, ASSIGNEE_ID),
                    caller))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("ASSIGNEE_NOT_MEMBER"));
    verify(taskRepository, never()).saveAndFlush(any());
  }

  @Test
  void updateStatus_member_changesStatus() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    Task task = taskInProject(PROJECT_ID);
    when(taskRepository.findByIdWithDetails(TASK_ID)).thenReturn(Optional.of(task));
    when(taskRepository.saveAndFlush(task)).thenReturn(task);

    // Act
    TaskDto dto =
        taskService.updateStatus(
            PROJECT_ID, TASK_ID, new UpdateTaskStatusRequest(TaskStatus.DONE), caller);

    // Assert
    assertThat(dto.status()).isEqualTo(TaskStatus.DONE);
    assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
  }

  @Test
  void deleteTask_member_deletesTask() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    Task task = taskInProject(PROJECT_ID);
    when(taskRepository.findByIdWithDetails(TASK_ID)).thenReturn(Optional.of(task));

    // Act
    taskService.deleteTask(PROJECT_ID, TASK_ID, caller);

    // Assert
    verify(projectAuthService).assertMember(PROJECT_ID, caller);
    verify(taskRepository).delete(task);
  }

  @Test
  void listTasks_member_assertsMembershipAndMapsResults() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    Task task = taskInProject(PROJECT_ID);
    when(taskRepository.findByProjectFiltered(PROJECT_ID, TaskStatus.TODO, null, null))
        .thenReturn(List.of(task));

    // Act
    List<TaskDto> tasks = taskService.listTasks(PROJECT_ID, TaskStatus.TODO, null, null, caller);

    // Assert
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).id()).isEqualTo(TASK_ID);
    verify(projectAuthService).assertMember(PROJECT_ID, caller);
  }

  @Test
  void statusCountsByProject_foldsRowsIntoPerProjectCounts() {
    // Arrange
    when(taskRepository.countByStatusInProjects(List.of(PROJECT_ID, 2L)))
        .thenReturn(
            List.of(
                TestData.taskStatusCount(PROJECT_ID, TaskStatus.TODO, 3),
                TestData.taskStatusCount(PROJECT_ID, TaskStatus.DONE, 1),
                TestData.taskStatusCount(2L, TaskStatus.IN_PROGRESS, 2)));

    // Act
    Map<Long, ProjectStatusCounts> counts =
        taskService.statusCountsByProject(List.of(PROJECT_ID, 2L));

    // Assert
    assertThat(counts.get(PROJECT_ID)).isEqualTo(new ProjectStatusCounts(3, 0, 1));
    assertThat(counts.get(2L)).isEqualTo(new ProjectStatusCounts(0, 2, 0));
  }

  @Test
  void statusCountsByProject_noProjects_returnsEmptyWithoutQuerying() {
    // Act
    Map<Long, ProjectStatusCounts> counts = taskService.statusCountsByProject(List.of());

    // Assert
    assertThat(counts).isEmpty();
    verifyNoInteractions(taskRepository);
  }

  @Test
  void listAssignedTasks_noProjects_returnsEmptyWithoutQuerying() {
    // Act
    List<TaskDto> tasks = taskService.listAssignedTasks(CALLER_ID, List.of());

    // Assert
    assertThat(tasks).isEmpty();
    verifyNoInteractions(taskRepository);
  }

  private Task taskInProject(Long projectId) {
    Project project = TestData.project(projectId, "Project " + projectId);
    User creator = TestData.user(CALLER_ID, "creator@test.com");
    return TestData.task(TASK_ID, project, creator);
  }
}
