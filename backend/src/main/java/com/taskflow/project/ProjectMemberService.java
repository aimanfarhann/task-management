package com.taskflow.project;

import com.taskflow.common.exception.ConflictException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.project.dto.AddMemberRequest;
import com.taskflow.project.dto.MemberDto;
import com.taskflow.project.dto.ProjectMapper;
import com.taskflow.user.User;
import com.taskflow.user.UserService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project membership business logic: listing, adding, and removing members. Enforces the
 * service-level invariant that every project keeps at least one OWNER (SCHEMA.md §2.3).
 */
@Service
public class ProjectMemberService {

  private static final Logger log = LoggerFactory.getLogger(ProjectMemberService.class);

  private final ProjectMemberRepository projectMemberRepository;
  private final ProjectRepository projectRepository;
  private final ProjectAuthService projectAuthService;
  private final UserService userService;

  /**
   * Creates the service.
   *
   * @param projectMemberRepository data access for memberships
   * @param projectRepository loads the project when adding members
   * @param projectAuthService project-scoped authorization checks
   * @param userService resolves the invited user by email
   */
  public ProjectMemberService(
      ProjectMemberRepository projectMemberRepository,
      ProjectRepository projectRepository,
      ProjectAuthService projectAuthService,
      UserService userService) {
    this.projectMemberRepository = projectMemberRepository;
    this.projectRepository = projectRepository;
    this.projectAuthService = projectAuthService;
    this.userService = userService;
  }

  /**
   * Lists a project's members. Member or ADMIN only.
   *
   * @param projectId the project id
   * @param currentUser the authenticated caller
   * @return the members ordered by join time
   */
  @Transactional(readOnly = true)
  public List<MemberDto> listMembers(Long projectId, AuthenticatedUser currentUser) {
    projectAuthService.assertMember(projectId, currentUser);
    return projectMemberRepository.findByProjectIdWithUser(projectId).stream()
        .map(ProjectMapper::toMemberDto)
        .toList();
  }

  /**
   * Adds a user to a project by email. OWNER or ADMIN only.
   *
   * @param projectId the project id
   * @param request the invited user's email and optional role (defaults to MEMBER)
   * @param currentUser the authenticated caller
   * @return the created membership
   * @throws ResourceNotFoundException with code {@code USER_NOT_FOUND} if no account has the email
   * @throws ConflictException with code {@code ALREADY_MEMBER} if the user is already a member
   */
  @Transactional
  public MemberDto addMember(
      Long projectId, AddMemberRequest request, AuthenticatedUser currentUser) {
    projectAuthService.assertOwner(projectId, currentUser);
    User user =
        userService
            .findByEmail(request.email())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "USER_NOT_FOUND", "No user found with that email"));
    if (projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
      throw new ConflictException("ALREADY_MEMBER", "User is already a member of this project");
    }
    Project project = loadProject(projectId);
    ProjectRole role = request.projectRole() == null ? ProjectRole.MEMBER : request.projectRole();
    try {
      ProjectMember membership =
          projectMemberRepository.saveAndFlush(new ProjectMember(project, user, role));
      log.info(
          "User {} added to project {} as {} by user {}",
          user.getId(),
          projectId,
          role,
          currentUser.id());
      return ProjectMapper.toMemberDto(membership);
    } catch (DataIntegrityViolationException e) {
      // A concurrent add of the same user passed the existence check above; the composite primary
      // key rejects the loser. Translate to the contract 409 rather than a 500.
      throw new ConflictException("ALREADY_MEMBER", "User is already a member of this project");
    }
  }

  /**
   * Removes a member from a project. OWNER/ADMIN may remove anyone; a plain MEMBER may only remove
   * themselves (leave). Removing the last OWNER is forbidden.
   *
   * @param projectId the project id
   * @param targetUserId the user to remove
   * @param currentUser the authenticated caller
   * @throws ResourceNotFoundException with code {@code MEMBER_NOT_FOUND} if the target is not a
   *     member
   * @throws ConflictException with code {@code LAST_OWNER} if the target is the only OWNER
   */
  @Transactional
  public void removeMember(Long projectId, Long targetUserId, AuthenticatedUser currentUser) {
    projectAuthService.assertMember(projectId, currentUser);
    boolean removingSelf = currentUser.id().equals(targetUserId);
    if (!removingSelf) {
      projectAuthService.assertOwner(projectId, currentUser);
    }
    // Serialize concurrent removals on this project so the last-OWNER count and the delete below
    // are atomic — otherwise two racing removals could both pass the check and leave zero owners.
    projectRepository.findByIdForUpdate(projectId);
    ProjectMember target =
        projectMemberRepository
            .findByProjectIdAndUserId(projectId, targetUserId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "MEMBER_NOT_FOUND",
                        "User " + targetUserId + " is not a member of project " + projectId));
    if (target.getProjectRole() == ProjectRole.OWNER && countOwners(projectId) <= 1) {
      throw new ConflictException("LAST_OWNER", "Cannot remove the last owner of a project");
    }
    projectMemberRepository.delete(target);
    log.info(
        "User {} removed from project {} by user {}", targetUserId, projectId, currentUser.id());
  }

  private long countOwners(Long projectId) {
    return projectMemberRepository.countByProjectIdAndProjectRole(projectId, ProjectRole.OWNER);
  }

  private Project loadProject(Long projectId) {
    // Existence was asserted above; this guards against a concurrent delete within the request.
    return projectRepository
        .findById(projectId)
        .orElseThrow(
            () -> new IllegalStateException("Project " + projectId + " vanished mid-request"));
  }
}
