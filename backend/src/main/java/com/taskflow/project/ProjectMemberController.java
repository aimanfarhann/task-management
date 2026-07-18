package com.taskflow.project;

import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.common.security.CurrentUser;
import com.taskflow.project.dto.AddMemberRequest;
import com.taskflow.project.dto.MemberDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoints for project membership management. */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/members")
public class ProjectMemberController {

  private final ProjectMemberService projectMemberService;

  /**
   * Creates the controller.
   *
   * @param projectMemberService the membership business logic
   */
  public ProjectMemberController(ProjectMemberService projectMemberService) {
    this.projectMemberService = projectMemberService;
  }

  /**
   * Lists a project's members. Member or ADMIN only.
   *
   * @param projectId the project id
   * @param currentUser the authenticated caller
   * @return 200 with the member list
   */
  @GetMapping
  public List<MemberDto> listMembers(
      @PathVariable Long projectId, @CurrentUser AuthenticatedUser currentUser) {
    return projectMemberService.listMembers(projectId, currentUser);
  }

  /**
   * Adds a user to the project by email. OWNER or ADMIN only.
   *
   * @param projectId the project id
   * @param request the invited user's email and optional role
   * @param currentUser the authenticated caller
   * @return 201 with the created membership
   */
  @PostMapping
  public ResponseEntity<MemberDto> addMember(
      @PathVariable Long projectId,
      @Valid @RequestBody AddMemberRequest request,
      @CurrentUser AuthenticatedUser currentUser) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(projectMemberService.addMember(projectId, request, currentUser));
  }

  /**
   * Removes a member. OWNER/ADMIN may remove anyone; a MEMBER may only remove themselves.
   *
   * @param projectId the project id
   * @param userId the user to remove
   * @param currentUser the authenticated caller
   * @return 204 with no body
   */
  @DeleteMapping("/{userId}")
  public ResponseEntity<Void> removeMember(
      @PathVariable Long projectId,
      @PathVariable Long userId,
      @CurrentUser AuthenticatedUser currentUser) {
    projectMemberService.removeMember(projectId, userId, currentUser);
    return ResponseEntity.noContent().build();
  }
}
