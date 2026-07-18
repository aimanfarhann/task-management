package com.taskflow.project;

import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.project.dto.CreateProjectRequest;
import com.taskflow.project.dto.ProjectDto;
import com.taskflow.project.dto.ProjectMapper;
import com.taskflow.project.dto.UpdateProjectRequest;
import com.taskflow.user.User;
import com.taskflow.user.UserService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project CRUD business logic. Authorization is asserted as the first line of every project-scoped
 * method (SCHEMA.md §4).
 */
@Service
public class ProjectService {

  private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final ProjectAuthService projectAuthService;
  private final UserService userService;

  /**
   * Creates the service.
   *
   * @param projectRepository data access for projects
   * @param projectMemberRepository data access for memberships
   * @param projectAuthService project-scoped authorization checks
   * @param userService loads the creator when establishing the OWNER membership
   */
  public ProjectService(
      ProjectRepository projectRepository,
      ProjectMemberRepository projectMemberRepository,
      ProjectAuthService projectAuthService,
      UserService userService) {
    this.projectRepository = projectRepository;
    this.projectMemberRepository = projectMemberRepository;
    this.projectAuthService = projectAuthService;
    this.userService = userService;
  }

  /**
   * Lists the caller's projects — every project they are a member of, archived included. A system
   * ADMIN sees all projects. Not project-scoped: the result is filtered to the caller by
   * construction, so no {@code assert*} applies.
   *
   * @param currentUser the authenticated caller
   * @return the caller's projects with their role and member counts
   */
  @Transactional(readOnly = true)
  public List<ProjectDto> listProjects(AuthenticatedUser currentUser) {
    List<ProjectMember> memberships =
        projectMemberRepository.findByUserIdWithProject(currentUser.id());
    Map<Long, ProjectRole> rolesByProjectId =
        memberships.stream()
            .collect(
                Collectors.toMap(
                    membership -> membership.getProject().getId(), ProjectMember::getProjectRole));

    List<Project> projects =
        currentUser.isAdmin()
            ? projectRepository.findAllByOrderByIdAsc()
            : memberships.stream().map(ProjectMember::getProject).toList();

    Map<Long, Long> counts = memberCountsFor(projects.stream().map(Project::getId).toList());
    return projects.stream()
        .map(
            project ->
                ProjectMapper.toDto(
                    project,
                    // ADMINs who are not members hold owner-level abilities — report OWNER.
                    rolesByProjectId.getOrDefault(project.getId(), ProjectRole.OWNER),
                    counts.getOrDefault(project.getId(), 0L)))
        .toList();
  }

  /**
   * Creates a project; the creator becomes its first OWNER member.
   *
   * @param request the validated project data
   * @param currentUser the authenticated caller
   * @return the created project with {@code myRole=OWNER} and {@code memberCount=1}
   */
  @Transactional
  public ProjectDto createProject(CreateProjectRequest request, AuthenticatedUser currentUser) {
    Project project =
        projectRepository.save(
            new Project(request.name(), request.description(), request.colorTag()));
    User creator =
        userService
            .findById(currentUser.id())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Authenticated user " + currentUser.id() + " not found"));
    projectMemberRepository.save(new ProjectMember(project, creator, ProjectRole.OWNER));
    log.info("Project {} created by user {}", project.getId(), currentUser.id());
    return ProjectMapper.toDto(project, ProjectRole.OWNER, 1);
  }

  /**
   * Returns a single project. Member or ADMIN only.
   *
   * @param projectId the project id
   * @param currentUser the authenticated caller
   * @return the project with the caller's role and member count
   */
  @Transactional(readOnly = true)
  public ProjectDto getProject(Long projectId, AuthenticatedUser currentUser) {
    projectAuthService.assertMember(projectId, currentUser);
    Project project = loadProject(projectId);
    return toDtoWithCallerContext(project, currentUser);
  }

  /**
   * Replaces a project's mutable fields (name, description, colorTag, archived). OWNER or ADMIN
   * only.
   *
   * @param projectId the project id
   * @param request the validated replacement data
   * @param currentUser the authenticated caller
   * @return the updated project with the caller's role and member count
   */
  @Transactional
  public ProjectDto updateProject(
      Long projectId, UpdateProjectRequest request, AuthenticatedUser currentUser) {
    projectAuthService.assertOwner(projectId, currentUser);
    Project project = loadProject(projectId);
    project.updateDetails(
        request.name(), request.description(), request.colorTag(), request.archived());
    log.info("Project {} updated by user {}", projectId, currentUser.id());
    return toDtoWithCallerContext(project, currentUser);
  }

  private Project loadProject(Long projectId) {
    // Existence was asserted above; this guards against a concurrent delete within the request.
    return projectRepository
        .findById(projectId)
        .orElseThrow(
            () -> new IllegalStateException("Project " + projectId + " vanished mid-request"));
  }

  private ProjectDto toDtoWithCallerContext(Project project, AuthenticatedUser currentUser) {
    ProjectRole myRole =
        projectMemberRepository
            .findByProjectIdAndUserId(project.getId(), currentUser.id())
            .map(ProjectMember::getProjectRole)
            // Only reachable for ADMINs — non-members failed the assert already. ADMINs hold
            // owner-level abilities, so report OWNER.
            .orElse(ProjectRole.OWNER);
    long memberCount = projectMemberRepository.countByProjectId(project.getId());
    return ProjectMapper.toDto(project, myRole, memberCount);
  }

  private Map<Long, Long> memberCountsFor(Collection<Long> projectIds) {
    if (projectIds.isEmpty()) {
      return Map.of();
    }
    return projectMemberRepository.countByProjectIds(projectIds).stream()
        .collect(
            Collectors.toMap(ProjectMemberCount::getProjectId, ProjectMemberCount::getMemberCount));
  }
}
