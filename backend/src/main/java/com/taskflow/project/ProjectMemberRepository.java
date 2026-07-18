package com.taskflow.project;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link ProjectMember}. Called only by this feature's services (RULES.md §21). */
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

  /**
   * Returns a user's memberships with their projects join-fetched, avoiding N+1 loads.
   *
   * @param userId the member's user id
   * @return the memberships ordered by project id
   */
  @Query(
      "select pm from ProjectMember pm join fetch pm.project where pm.user.id = :userId"
          + " order by pm.project.id")
  List<ProjectMember> findByUserIdWithProject(@Param("userId") Long userId);

  /**
   * Returns a project's memberships with their users join-fetched, avoiding N+1 loads.
   *
   * @param projectId the project id
   * @return the memberships ordered by join time, then user id for a stable order
   */
  @Query(
      "select pm from ProjectMember pm join fetch pm.user where pm.project.id = :projectId"
          + " order by pm.joinedAt, pm.user.id")
  List<ProjectMember> findByProjectIdWithUser(@Param("projectId") Long projectId);

  /**
   * Finds a single membership.
   *
   * @param projectId the project id
   * @param userId the user id
   * @return the membership, or empty if the user is not a member
   */
  Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

  /**
   * Returns whether a user is a member of a project.
   *
   * @param projectId the project id
   * @param userId the user id
   * @return true if a membership exists
   */
  boolean existsByProjectIdAndUserId(Long projectId, Long userId);

  /**
   * Counts the members of a project.
   *
   * @param projectId the project id
   * @return the number of memberships
   */
  long countByProjectId(Long projectId);

  /**
   * Counts a project's members holding the given role — used for the last-OWNER invariant.
   *
   * @param projectId the project id
   * @param projectRole the role to count
   * @return the number of members with that role
   */
  long countByProjectIdAndProjectRole(Long projectId, ProjectRole projectRole);

  /**
   * Returns member counts for many projects in one query — used by the project listing.
   *
   * @param projectIds the project ids to count members for
   * @return one row per project that has members
   */
  @Query(
      "select pm.project.id as projectId, count(pm) as memberCount from ProjectMember pm"
          + " where pm.project.id in :projectIds group by pm.project.id")
  List<ProjectMemberCount> countByProjectIds(@Param("projectIds") Collection<Long> projectIds);
}
