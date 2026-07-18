package com.taskflow.project;

import com.taskflow.common.exception.ForbiddenException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.common.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project-scoped authorization checks (SCHEMA.md §4). Every service method touching project data
 * calls one of these as its first line. A nonexistent project yields 404 before any membership
 * check; a missing membership yields 403. System ADMINs pass every check.
 */
@Service
public class ProjectAuthService {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;

  /**
   * Creates the service.
   *
   * @param projectRepository verifies project existence
   * @param projectMemberRepository verifies membership and roles
   */
  public ProjectAuthService(
      ProjectRepository projectRepository, ProjectMemberRepository projectMemberRepository) {
    this.projectRepository = projectRepository;
    this.projectMemberRepository = projectMemberRepository;
  }

  /**
   * Asserts the user may read the project: any member, or a system ADMIN.
   *
   * @param projectId the project being accessed
   * @param user the authenticated caller
   * @throws ResourceNotFoundException if the project does not exist (404)
   * @throws ForbiddenException if the user is neither a member nor an ADMIN (403)
   */
  @Transactional(readOnly = true)
  public void assertMember(Long projectId, AuthenticatedUser user) {
    assertProjectExists(projectId);
    if (user.isAdmin()) {
      return;
    }
    if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, user.id())) {
      throw new ForbiddenException("You are not a member of this project");
    }
  }

  /**
   * Asserts the user may administer the project: an OWNER member, or a system ADMIN.
   *
   * @param projectId the project being administered
   * @param user the authenticated caller
   * @throws ResourceNotFoundException if the project does not exist (404)
   * @throws ForbiddenException if the user is not an OWNER (including non-members) (403)
   */
  @Transactional(readOnly = true)
  public void assertOwner(Long projectId, AuthenticatedUser user) {
    assertProjectExists(projectId);
    if (user.isAdmin()) {
      return;
    }
    ProjectRole role =
        projectMemberRepository
            .findByProjectIdAndUserId(projectId, user.id())
            .map(ProjectMember::getProjectRole)
            .orElse(null);
    if (role != ProjectRole.OWNER) {
      throw new ForbiddenException("Only a project owner may perform this action");
    }
  }

  /**
   * Returns whether a user is currently a member of a project. Membership is by explicit
   * project_members row only — a system ADMIN is not counted as a member here. Used to validate
   * that a task assignee actually belongs to the project.
   *
   * @param projectId the project id
   * @param userId the user id to check
   * @return true if the user holds a membership in the project
   */
  @Transactional(readOnly = true)
  public boolean isMember(Long projectId, Long userId) {
    return projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
  }

  private void assertProjectExists(Long projectId) {
    if (!projectRepository.existsById(projectId)) {
      throw new ResourceNotFoundException(
          "PROJECT_NOT_FOUND", "Project " + projectId + " was not found");
    }
  }
}
