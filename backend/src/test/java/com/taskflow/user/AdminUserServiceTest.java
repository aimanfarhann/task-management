package com.taskflow.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.taskflow.common.exception.ConflictException;
import com.taskflow.common.exception.ForbiddenException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.testutil.TestData;
import com.taskflow.user.dto.AdminUserDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link AdminUserService} business rules. */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private AdminUserService adminUserService;

  @Test
  void listAllUsers_asAdmin_returnsMappedUsersInRepositoryOrder() {
    // Arrange
    AuthenticatedUser admin = TestData.authAdmin(1L);
    when(userRepository.findAllByOrderByCreatedAtAscIdAsc())
        .thenReturn(
            List.of(TestData.user(1L, "first@test.com"), TestData.admin(2L, "second@test.com")));

    // Act
    List<AdminUserDto> users = adminUserService.listAllUsers(admin);

    // Assert: mapping preserves repository order and never leaks the entity.
    assertThat(users).hasSize(2);
    assertThat(users.get(0).id()).isEqualTo(1L);
    assertThat(users.get(0).email()).isEqualTo("first@test.com");
    assertThat(users.get(0).role()).isEqualTo(Role.USER);
    assertThat(users.get(0).active()).isTrue();
    assertThat(users.get(1).id()).isEqualTo(2L);
    assertThat(users.get(1).role()).isEqualTo(Role.ADMIN);
  }

  @Test
  void listAllUsers_asNonAdmin_throwsForbiddenBeforeAnyLookup() {
    // Arrange
    AuthenticatedUser plainUser = TestData.authUser(1L);

    // Act + Assert
    assertThatThrownBy(() -> adminUserService.listAllUsers(plainUser))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(userRepository);
  }

  @Test
  void setActive_deactivateOtherUser_asAdmin_deactivatesAndReturnsDto() {
    // Arrange
    AuthenticatedUser admin = TestData.authAdmin(1L);
    User target = TestData.user(2L, "target@test.com");
    when(userRepository.findById(2L)).thenReturn(Optional.of(target));

    // Act
    AdminUserDto result = adminUserService.setActive(2L, false, admin);

    // Assert
    assertThat(result.active()).isFalse();
    assertThat(target.isActive()).isFalse();
  }

  @Test
  void setActive_reactivateSelf_asAdmin_isAllowed() {
    // Arrange: reactivation is always allowed, even for the admin's own account.
    AuthenticatedUser admin = TestData.authAdmin(1L);
    User self = TestData.inactiveUser(1L, "admin@test.com");
    when(userRepository.findById(1L)).thenReturn(Optional.of(self));

    // Act
    AdminUserDto result = adminUserService.setActive(1L, true, admin);

    // Assert
    assertThat(result.active()).isTrue();
    assertThat(self.isActive()).isTrue();
  }

  @Test
  void setActive_unknownUser_throwsUserNotFound() {
    // Arrange
    AuthenticatedUser admin = TestData.authAdmin(1L);
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    // Act + Assert
    assertThatThrownBy(() -> adminUserService.setActive(999L, false, admin))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("USER_NOT_FOUND"));
  }

  @Test
  void setActive_adminDeactivatingSelf_throwsCannotDeactivateSelf() {
    // Arrange
    AuthenticatedUser admin = TestData.authAdmin(1L);
    User self = TestData.admin(1L, "admin@test.com");
    when(userRepository.findById(1L)).thenReturn(Optional.of(self));

    // Act + Assert: the self-lockout guard rejects the request and leaves the account untouched.
    assertThatThrownBy(() -> adminUserService.setActive(1L, false, admin))
        .isInstanceOfSatisfying(
            ConflictException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("CANNOT_DEACTIVATE_SELF"));
    assertThat(self.isActive()).isTrue();
  }

  @Test
  void setActive_deactivatingAdminWhenAnotherAdminRemains_succeeds() {
    // Arrange: two active admins exist, so deactivating one leaves the other.
    AuthenticatedUser actingAdmin = TestData.authAdmin(1L);
    User targetAdmin = TestData.admin(2L, "target-admin@test.com");
    when(userRepository.findById(2L)).thenReturn(Optional.of(targetAdmin));
    when(userRepository.findActiveByRoleForUpdate(Role.ADMIN))
        .thenReturn(List.of(TestData.admin(1L, "acting@test.com"), targetAdmin));

    // Act
    AdminUserDto result = adminUserService.setActive(2L, false, actingAdmin);

    // Assert
    assertThat(result.active()).isFalse();
    assertThat(targetAdmin.isActive()).isFalse();
  }

  @Test
  void setActive_deactivatingLastActiveAdmin_throwsCannotDeactivateLastAdmin() {
    // Arrange: the locking read sees the target as the only active admin (e.g. the acting admin was
    // concurrently deactivated), so removing it would leave zero admins.
    AuthenticatedUser actingAdmin = TestData.authAdmin(1L);
    User targetAdmin = TestData.admin(2L, "last-admin@test.com");
    when(userRepository.findById(2L)).thenReturn(Optional.of(targetAdmin));
    when(userRepository.findActiveByRoleForUpdate(Role.ADMIN)).thenReturn(List.of(targetAdmin));

    // Act + Assert: the last-admin guard rejects the request and leaves the account active.
    assertThatThrownBy(() -> adminUserService.setActive(2L, false, actingAdmin))
        .isInstanceOfSatisfying(
            ConflictException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("CANNOT_DEACTIVATE_LAST_ADMIN"));
    assertThat(targetAdmin.isActive()).isTrue();
  }

  @Test
  void setActive_asNonAdmin_throwsForbiddenBeforeAnyLookup() {
    // Arrange
    AuthenticatedUser plainUser = TestData.authUser(1L);

    // Act + Assert
    assertThatThrownBy(() -> adminUserService.setActive(2L, false, plainUser))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(userRepository);
  }
}
