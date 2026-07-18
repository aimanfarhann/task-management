package com.taskflow.task;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link Task}. Called only by this feature's services (RULES.md §21). */
public interface TaskRepository extends JpaRepository<Task, Long> {

  /**
   * Lists a project's tasks with optional status/priority/assignee filters applied server-side. A
   * null filter means "no restriction". Assignee and creator are join-fetched to avoid N+1 loads
   * (SCHEMA.md §6). Ordered by creation time for a stable listing.
   *
   * @param projectId the project whose tasks are listed
   * @param status the status to filter by, or null for all statuses
   * @param priority the priority to filter by, or null for all priorities
   * @param assigneeId the assignee to filter by, or null for any assignee
   * @return the matching tasks, oldest first
   */
  @Query(
      "select t from Task t join fetch t.createdBy left join fetch t.assignee a"
          + " where t.project.id = :projectId"
          + " and (:status is null or t.status = :status)"
          + " and (:priority is null or t.priority = :priority)"
          + " and (:assigneeId is null or a.id = :assigneeId)"
          + " order by t.createdAt asc, t.id asc")
  List<Task> findByProjectFiltered(
      @Param("projectId") Long projectId,
      @Param("status") TaskStatus status,
      @Param("priority") TaskPriority priority,
      @Param("assigneeId") Long assigneeId);

  /**
   * Loads a single task with its assignee and creator join-fetched, so it can be mapped or mutated
   * without lazy-loading round trips.
   *
   * @param id the task id
   * @return the task, or empty if the id is unknown
   */
  @Query("select t from Task t join fetch t.createdBy left join fetch t.assignee where t.id = :id")
  Optional<Task> findByIdWithDetails(@Param("id") Long id);

  /**
   * Returns the tasks assigned to a user across the given projects, for the dashboard. Assignee and
   * creator are join-fetched. Ordered by due date (nulls last) then id.
   *
   * @param userId the assignee's user id
   * @param projectIds the projects to search within
   * @return the user's assigned tasks
   */
  @Query(
      "select t from Task t join fetch t.createdBy left join fetch t.assignee a"
          + " where a.id = :userId and t.project.id in :projectIds"
          + " order by t.dueDate asc nulls last, t.id asc")
  List<Task> findAssignedInProjects(
      @Param("userId") Long userId, @Param("projectIds") Collection<Long> projectIds);

  /**
   * Returns task counts grouped by project and status across the given projects, for the dashboard
   * per-project summaries.
   *
   * @param projectIds the projects to count within
   * @return one row per (project, status) pair that has at least one task
   */
  @Query(
      "select t.project.id as projectId, t.status as status, count(t) as count"
          + " from Task t where t.project.id in :projectIds"
          + " group by t.project.id, t.status")
  List<TaskStatusCount> countByStatusInProjects(@Param("projectIds") Collection<Long> projectIds);
}
