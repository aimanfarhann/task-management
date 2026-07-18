package com.taskflow.project.dto;

import com.taskflow.project.Project;
import com.taskflow.project.ProjectMember;
import com.taskflow.project.ProjectRole;

/** Manual entity-to-DTO mapping for projects and members (ARCHITECTURE.md §3.1 — no MapStruct). */
public final class ProjectMapper {

  private ProjectMapper() {}

  /**
   * Maps a project entity plus caller context to its API representation.
   *
   * @param project the persisted project entity
   * @param myRole the calling user's role in the project
   * @param memberCount the project's member count
   * @return the contract-shaped DTO
   */
  public static ProjectDto toDto(Project project, ProjectRole myRole, long memberCount) {
    return new ProjectDto(
        project.getId(),
        project.getName(),
        project.getDescription(),
        project.getColorTag(),
        project.isArchived(),
        project.getCreatedAt(),
        myRole,
        memberCount);
  }

  /**
   * Maps a membership (with its user association loaded) to its API representation.
   *
   * @param member the persisted membership entity
   * @return the contract-shaped DTO
   */
  public static MemberDto toMemberDto(ProjectMember member) {
    return new MemberDto(
        member.getUser().getId(),
        member.getUser().getEmail(),
        member.getUser().getDisplayName(),
        member.getProjectRole(),
        member.getJoinedAt());
  }
}
