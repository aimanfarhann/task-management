package com.taskflow.dashboard;

import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.dashboard.dto.DashboardDto;
import com.taskflow.dashboard.dto.DashboardMapper;
import com.taskflow.dashboard.dto.DashboardTaskDto;
import com.taskflow.dashboard.dto.ProjectSummaryDto;
import com.taskflow.project.ProjectService;
import com.taskflow.project.dto.ProjectDto;
import com.taskflow.task.ProjectStatusCounts;
import com.taskflow.task.TaskService;
import com.taskflow.task.dto.TaskDto;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles the dashboard for the caller (PRD §5.4). Aggregates read-only data across the project
 * and task features through their services (ARCHITECTURE.md §3.2 — never their repositories).
 */
@Service
public class DashboardService {

  private final ProjectService projectService;
  private final TaskService taskService;

  /**
   * Creates the service.
   *
   * @param projectService supplies the caller's project memberships
   * @param taskService supplies the caller's assigned tasks and per-project status counts
   */
  public DashboardService(ProjectService projectService, TaskService taskService) {
    this.projectService = projectService;
    this.taskService = taskService;
  }

  /**
   * Builds the caller's dashboard: the tasks assigned to them across the projects they belong to,
   * and a status rollup for each of those projects.
   *
   * @param currentUser the authenticated caller
   * @return the assembled dashboard
   */
  @Transactional(readOnly = true)
  public DashboardDto getDashboard(AuthenticatedUser currentUser) {
    List<ProjectDto> myProjects = projectService.listMembershipProjects(currentUser.id());
    List<Long> projectIds = myProjects.stream().map(ProjectDto::id).toList();
    Map<Long, ProjectDto> projectsById =
        myProjects.stream().collect(Collectors.toMap(ProjectDto::id, Function.identity()));

    List<DashboardTaskDto> myTasks =
        taskService.listAssignedTasks(currentUser.id(), projectIds).stream()
            .map(task -> toDashboardTask(task, projectsById))
            .toList();

    Map<Long, ProjectStatusCounts> countsByProject = taskService.statusCountsByProject(projectIds);
    List<ProjectSummaryDto> projectSummaries =
        myProjects.stream().map(project -> toSummary(project, countsByProject)).toList();

    return new DashboardDto(myTasks, projectSummaries);
  }

  private DashboardTaskDto toDashboardTask(TaskDto task, Map<Long, ProjectDto> projectsById) {
    ProjectDto project = projectsById.get(task.projectId());
    return DashboardMapper.toDashboardTask(task, project.name(), project.colorTag());
  }

  private ProjectSummaryDto toSummary(
      ProjectDto project, Map<Long, ProjectStatusCounts> countsByProject) {
    ProjectStatusCounts counts =
        countsByProject.getOrDefault(project.id(), ProjectStatusCounts.ZERO);
    return new ProjectSummaryDto(
        project.id(),
        project.name(),
        project.colorTag(),
        counts.todoCount(),
        counts.inProgressCount(),
        counts.doneCount());
  }
}
