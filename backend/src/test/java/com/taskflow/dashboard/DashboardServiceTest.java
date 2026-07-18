package com.taskflow.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.dashboard.dto.DashboardDto;
import com.taskflow.dashboard.dto.DashboardTaskDto;
import com.taskflow.dashboard.dto.ProjectSummaryDto;
import com.taskflow.project.ProjectRole;
import com.taskflow.project.ProjectService;
import com.taskflow.project.dto.ProjectDto;
import com.taskflow.task.ProjectStatusCounts;
import com.taskflow.task.TaskPriority;
import com.taskflow.task.TaskService;
import com.taskflow.task.TaskStatus;
import com.taskflow.task.dto.TaskDto;
import com.taskflow.task.dto.UserSummary;
import com.taskflow.testutil.TestData;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link DashboardService} assembly across the project and task features. */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

  private static final Long CALLER_ID = 7L;

  @Mock private ProjectService projectService;
  @Mock private TaskService taskService;

  @InjectMocks private DashboardService dashboardService;

  @Test
  void getDashboard_assemblesAssignedTasksAndPerProjectSummaries() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    ProjectDto alpha = projectDto(1L, "Alpha", "blue");
    ProjectDto beta = projectDto(2L, "Beta", "red");
    when(projectService.listMembershipProjects(CALLER_ID)).thenReturn(List.of(alpha, beta));
    when(taskService.listAssignedTasks(CALLER_ID, List.of(1L, 2L)))
        .thenReturn(List.of(taskDto(100L, 1L)));
    when(taskService.statusCountsByProject(List.of(1L, 2L)))
        .thenReturn(Map.of(1L, new ProjectStatusCounts(2, 1, 0)));

    // Act
    DashboardDto dashboard = dashboardService.getDashboard(caller);

    // Assert: assigned task carries its project's name and color.
    assertThat(dashboard.myTasks()).hasSize(1);
    DashboardTaskDto myTask = dashboard.myTasks().get(0);
    assertThat(myTask.id()).isEqualTo(100L);
    assertThat(myTask.projectId()).isEqualTo(1L);
    assertThat(myTask.projectName()).isEqualTo("Alpha");
    assertThat(myTask.projectColorTag()).isEqualTo("blue");

    // Assert: one summary per membership; a project with no counts falls back to zero.
    assertThat(dashboard.projectSummaries()).hasSize(2);
    ProjectSummaryDto alphaSummary = dashboard.projectSummaries().get(0);
    assertThat(alphaSummary.projectId()).isEqualTo(1L);
    assertThat(alphaSummary.todoCount()).isEqualTo(2);
    assertThat(alphaSummary.inProgressCount()).isEqualTo(1);
    assertThat(alphaSummary.doneCount()).isEqualTo(0);
    ProjectSummaryDto betaSummary = dashboard.projectSummaries().get(1);
    assertThat(betaSummary.projectId()).isEqualTo(2L);
    assertThat(betaSummary.todoCount()).isEqualTo(0);
    assertThat(betaSummary.inProgressCount()).isEqualTo(0);
    assertThat(betaSummary.doneCount()).isEqualTo(0);
  }

  @Test
  void getDashboard_noMemberships_returnsEmptyDashboard() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    when(projectService.listMembershipProjects(CALLER_ID)).thenReturn(List.of());
    when(taskService.listAssignedTasks(CALLER_ID, List.of())).thenReturn(List.of());
    when(taskService.statusCountsByProject(List.of())).thenReturn(Map.of());

    // Act
    DashboardDto dashboard = dashboardService.getDashboard(caller);

    // Assert
    assertThat(dashboard.myTasks()).isEmpty();
    assertThat(dashboard.projectSummaries()).isEmpty();
  }

  private ProjectDto projectDto(Long id, String name, String colorTag) {
    return new ProjectDto(id, name, null, colorTag, false, Instant.now(), ProjectRole.MEMBER, 1L);
  }

  private TaskDto taskDto(Long id, Long projectId) {
    return new TaskDto(
        id,
        projectId,
        "Task " + id,
        null,
        TaskStatus.TODO,
        TaskPriority.MEDIUM,
        null,
        new UserSummary(CALLER_ID, "Test User"),
        new UserSummary(CALLER_ID, "Test User"),
        Instant.now(),
        Instant.now());
  }
}
