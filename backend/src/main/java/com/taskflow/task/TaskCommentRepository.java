package com.taskflow.task;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link TaskComment}. Called only by this feature's services (RULES.md §21). */
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

  /**
   * Returns a task's comments in chronological order, with each author join-fetched to avoid N+1
   * loads.
   *
   * @param taskId the task id
   * @return the comments oldest first
   */
  @Query(
      "select c from TaskComment c join fetch c.author where c.task.id = :taskId"
          + " order by c.createdAt asc, c.id asc")
  List<TaskComment> findByTaskIdWithAuthor(@Param("taskId") Long taskId);

  /**
   * Loads a single comment with its author join-fetched, for the delete authorization check.
   *
   * @param id the comment id
   * @return the comment, or empty if the id is unknown
   */
  @Query("select c from TaskComment c join fetch c.author where c.id = :id")
  Optional<TaskComment> findByIdWithAuthor(@Param("id") Long id);
}
