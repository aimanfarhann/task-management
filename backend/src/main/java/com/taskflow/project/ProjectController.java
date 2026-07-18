package com.taskflow.project;

import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.common.security.CurrentUser;
import com.taskflow.project.dto.CreateProjectRequest;
import com.taskflow.project.dto.ProjectDto;
import com.taskflow.project.dto.UpdateProjectRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoints for project CRUD. */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

  private final ProjectService projectService;

  /**
   * Creates the controller.
   *
   * @param projectService the project business logic
   */
  public ProjectController(ProjectService projectService) {
    this.projectService = projectService;
  }

  /**
   * Lists the caller's projects (all projects for ADMINs), archived included.
   *
   * @param currentUser the authenticated caller
   * @return 200 with the project list
   */
  @GetMapping
  public List<ProjectDto> listProjects(@CurrentUser AuthenticatedUser currentUser) {
    return projectService.listProjects(currentUser);
  }

  /**
   * Creates a project owned by the caller.
   *
   * @param request the project data
   * @param currentUser the authenticated caller
   * @return 201 with the created project
   */
  @PostMapping
  public ResponseEntity<ProjectDto> createProject(
      @Valid @RequestBody CreateProjectRequest request,
      @CurrentUser AuthenticatedUser currentUser) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(projectService.createProject(request, currentUser));
  }

  /**
   * Returns a single project. Member or ADMIN only.
   *
   * @param id the project id
   * @param currentUser the authenticated caller
   * @return 200 with the project
   */
  @GetMapping("/{id}")
  public ProjectDto getProject(@PathVariable Long id, @CurrentUser AuthenticatedUser currentUser) {
    return projectService.getProject(id, currentUser);
  }

  /**
   * Replaces a project's mutable fields. OWNER or ADMIN only.
   *
   * @param id the project id
   * @param request the replacement data
   * @param currentUser the authenticated caller
   * @return 200 with the updated project
   */
  @PutMapping("/{id}")
  public ProjectDto updateProject(
      @PathVariable Long id,
      @Valid @RequestBody UpdateProjectRequest request,
      @CurrentUser AuthenticatedUser currentUser) {
    return projectService.updateProject(id, request, currentUser);
  }
}
