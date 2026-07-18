package com.taskflow.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.taskflow.IntegrationTestBase;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/** Repository tests for {@link UserRepository} against Testcontainers PostgreSQL. */
class UserRepositoryTest extends IntegrationTestBase {

  @Autowired private UserRepository userRepository;

  @Test
  void findByEmailIgnoreCase_differentCasing_findsUser() {
    String email = "repo-find-" + UUID.randomUUID() + "@test.com";
    userRepository.save(new User(email, "$2a$10$hash", "Repo User"));

    Optional<User> found = userRepository.findByEmailIgnoreCase(email.toUpperCase());

    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo(email);
  }

  @Test
  void existsByEmailIgnoreCase_differentCasing_returnsTrue() {
    String email = "repo-exists-" + UUID.randomUUID() + "@test.com";
    userRepository.save(new User(email, "$2a$10$hash", "Repo User"));

    assertThat(userRepository.existsByEmailIgnoreCase(email.toUpperCase())).isTrue();
    assertThat(userRepository.existsByEmailIgnoreCase("other-" + email)).isFalse();
  }

  @Test
  void save_duplicateEmailDifferentCase_violatesUniqueLowerEmailIndex() {
    String email = "repo-dup-" + UUID.randomUUID() + "@test.com";
    userRepository.saveAndFlush(new User(email, "$2a$10$hash", "First"));

    assertThatThrownBy(
            () ->
                userRepository.saveAndFlush(new User(email.toUpperCase(), "$2a$10$hash", "Second")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
