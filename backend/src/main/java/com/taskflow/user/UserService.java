package com.taskflow.user;

import com.taskflow.common.exception.DuplicateEmailException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User domain service. Other features access user data exclusively through this service, never
 * through {@link UserRepository} directly (ARCHITECTURE.md §3.2).
 */
@Service
public class UserService {

  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  /**
   * Creates the service.
   *
   * @param userRepository data access for users
   */
  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Creates a new user, enforcing case-insensitive email uniqueness.
   *
   * @param email the email address
   * @param passwordHash the BCrypt hash of the password — hashing happens in the auth feature
   * @param displayName the display name
   * @return the persisted user
   * @throws DuplicateEmailException if the email is already taken in any casing
   */
  @Transactional
  public User createUser(String email, String passwordHash, String displayName) {
    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new DuplicateEmailException();
    }
    try {
      User user = userRepository.saveAndFlush(new User(email, passwordHash, displayName));
      log.info("Created user {}", user.getId());
      return user;
    } catch (DataIntegrityViolationException e) {
      // Two concurrent registrations both passed the existence check above; the unique index on
      // lower(email) rejects the loser. Translate to the contract 409 rather than a 500.
      throw new DuplicateEmailException();
    }
  }

  /**
   * Finds a user by email, ignoring case.
   *
   * @param email the email address
   * @return the user, or empty if no account uses this email
   */
  @Transactional(readOnly = true)
  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmailIgnoreCase(email);
  }

  /**
   * Finds a user by id.
   *
   * @param id the user's database id
   * @return the user, or empty if the id is unknown
   */
  @Transactional(readOnly = true)
  public Optional<User> findById(Long id) {
    return userRepository.findById(id);
  }
}
