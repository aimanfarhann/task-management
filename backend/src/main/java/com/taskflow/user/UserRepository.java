package com.taskflow.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link User}. Called only by {@link UserService} (RULES.md §21). */
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
}
