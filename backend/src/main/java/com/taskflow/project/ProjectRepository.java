package com.taskflow.project;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link Project}. Called only by this feature's services (RULES.md §21). */
public interface ProjectRepository extends JpaRepository<Project, Long> {

  /**
   * Returns every project ordered by id — used by the ADMIN listing.
   *
   * @return all projects, oldest first
   */
  List<Project> findAllByOrderByIdAsc();

  /**
   * Loads a project under a pessimistic write lock, serializing membership mutations on it. Used to
   * make the last-OWNER invariant race-safe: concurrent member removals on the same project queue
   * behind this lock so each re-evaluates the owner count against committed state.
   *
   * @param id the project id
   * @return the locked project, or empty if it does not exist
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Project p where p.id = :id")
  Optional<Project> findByIdForUpdate(@Param("id") Long id);
}
