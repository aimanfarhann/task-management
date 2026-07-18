package com.taskflow.user;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link User}. Called only by the {@code user} feature's services — {@link
 * UserService} and {@link AdminUserService} (RULES.md §21).
 */
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Finds a user by email, ignoring case — email uniqueness is case-insensitive (SCHEMA.md §2.1).
   *
   * @param email the email address to look up
   * @return the user, or empty if no account uses this email
   */
  Optional<User> findByEmailIgnoreCase(String email);

  /**
   * Returns whether an account already uses this email, ignoring case.
   *
   * @param email the email address to check
   * @return true if a user exists with this email in any casing
   */
  boolean existsByEmailIgnoreCase(String email);

  /**
   * Lists every user for the admin user-management page, oldest first with id as a stable
   * tie-breaker (M3 admin contract).
   *
   * @return all users ordered by creation time ascending, then id ascending
   */
  List<User> findAllByOrderByCreatedAtAscIdAsc();

  /**
   * Loads the currently-active users holding the given role under a pessimistic write lock,
   * serializing concurrent role-membership changes. Used to make the last-admin invariant
   * race-safe: concurrent admin deactivations queue behind this lock so each re-evaluates against
   * committed state (analogous to the last-OWNER guard, SCHEMA.md §4).
   *
   * @param role the role to lock the active holders of
   * @return the active users with that role, write-locked for the transaction
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.role = :role and u.active = true")
  List<User> findActiveByRoleForUpdate(@Param("role") Role role);
}
