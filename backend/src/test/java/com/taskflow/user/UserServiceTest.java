package com.taskflow.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskflow.common.exception.DuplicateEmailException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/** Unit tests for {@link UserService}. */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UserService userService;

  @Test
  void createUser_newEmail_persistsActiveUserWithDefaultRole() {
    // Arrange
    when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
    when(userRepository.saveAndFlush(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    User created = userService.createUser("new@test.com", "$2a$10$hash", "New User");

    // Assert
    assertThat(created.getEmail()).isEqualTo("new@test.com");
    assertThat(created.getRole()).isEqualTo(Role.USER);
    assertThat(created.isActive()).isTrue();
    assertThat(created.getPasswordHash()).isEqualTo("$2a$10$hash");
  }

  @Test
  void createUser_duplicateEmail_throwsDuplicateEmailException() {
    // Arrange
    when(userRepository.existsByEmailIgnoreCase("taken@test.com")).thenReturn(true);

    // Act + Assert
    assertThatThrownBy(() -> userService.createUser("taken@test.com", "hash", "Name"))
        .isInstanceOf(DuplicateEmailException.class);
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  void createUser_concurrentDuplicate_translatesConstraintViolationToConflict() {
    // Arrange: the existence check passes, but a racing insert already took the email, so the
    // unique index rejects this one at flush time.
    when(userRepository.existsByEmailIgnoreCase("race@test.com")).thenReturn(false);
    when(userRepository.saveAndFlush(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("unique index on lower(email)"));

    // Act + Assert
    assertThatThrownBy(() -> userService.createUser("race@test.com", "hash", "Name"))
        .isInstanceOf(DuplicateEmailException.class);
  }
}
