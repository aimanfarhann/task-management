package com.taskflow.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskflow.common.exception.ForbiddenException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.testutil.TestData;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the {@link ProjectAuthService} authorization primitives (SCHEMA.md §4). */
@ExtendWith(MockitoExtension.class)
class ProjectAuthServiceTest {

  private static final Long PROJECT_ID = 10L;

  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectMemberRepository projectMemberRepository;

  @InjectMocks private ProjectAuthService projectAuthService;

  @Test
  void assertMember_unknownProject_throwsProjectNotFound() {
    // Arrange
    when(projectRepository.existsById(PROJECT_ID)).thenReturn(false);

    // Act + Assert
    assertThatThrownBy(() -> projectAuthService.assertMember(PROJECT_ID, TestData.authUser(1L)))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("PROJECT_NOT_FOUND"));
  }

  @Test
  void assertMember_nonMember_throwsForbidden() {
    // Arrange
    when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
    when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 1L)).thenReturn(false);

    // Act + Assert
    assertThatThrownBy(() -> projectAuthService.assertMember(PROJECT_ID, TestData.authUser(1L)))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void assertMember_member_passes() {
    // Arrange
    when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
    when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 1L)).thenReturn(true);

    // Act + Assert
    assertThatCode(() -> projectAuthService.assertMember(PROJECT_ID, TestData.authUser(1L)))
        .doesNotThrowAnyException();
  }

  @Test
  void assertMember_adminWithoutMembership_passesWithoutMembershipLookup() {
    // Arrange
    when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);

    // Act
    projectAuthService.assertMember(PROJECT_ID, TestData.authAdmin(99L));

    // Assert
    verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyLong(), anyLong());
  }

  @Test
  void assertOwner_unknownProject_throwsProjectNotFound() {
    // Arrange
    when(projectRepository.existsById(PROJECT_ID)).thenReturn(false);

    // Act + Assert
    assertThatThrownBy(() -> projectAuthService.assertOwner(PROJECT_ID, TestData.authUser(1L)))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("PROJECT_NOT_FOUND"));
  }

  @Test
  void assertOwner_plainMember_throwsForbidden() {
    // Arrange
    when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
    ProjectMember membership =
        TestData.membership(
            TestData.project(PROJECT_ID, "Project"),
            TestData.user(1L, "member@test.com"),
            ProjectRole.MEMBER);
    when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, 1L))
        .thenReturn(Optional.of(membership));

    // Act + Assert
    assertThatThrownBy(() -> projectAuthService.assertOwner(PROJECT_ID, TestData.authUser(1L)))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void assertOwner_nonMember_throwsForbidden() {
    // Arrange
    when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
    when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, 1L))
        .thenReturn(Optional.empty());

    // Act + Assert
    assertThatThrownBy(() -> projectAuthService.assertOwner(PROJECT_ID, TestData.authUser(1L)))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void assertOwner_owner_passes() {
    // Arrange
    when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
    ProjectMember membership =
        TestData.membership(
            TestData.project(PROJECT_ID, "Project"),
            TestData.user(1L, "owner@test.com"),
            ProjectRole.OWNER);
    when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, 1L))
        .thenReturn(Optional.of(membership));

    // Act + Assert
    assertThatCode(() -> projectAuthService.assertOwner(PROJECT_ID, TestData.authUser(1L)))
        .doesNotThrowAnyException();
  }

  @Test
  void assertOwner_adminWithoutMembership_passesWithoutMembershipLookup() {
    // Arrange
    when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);

    // Act
    projectAuthService.assertOwner(PROJECT_ID, TestData.authAdmin(99L));

    // Assert
    verify(projectMemberRepository, never()).findByProjectIdAndUserId(anyLong(), anyLong());
    verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyLong(), anyLong());
  }

  @Test
  void assertChecks_runExistenceBeforeMembership() {
    // Arrange: nonexistent project must yield 404 even for a non-member (contract: 404 over 403).
    when(projectRepository.existsById(PROJECT_ID)).thenReturn(false);
    AuthenticatedUser nonMember = TestData.authUser(1L);

    // Act + Assert
    assertThatThrownBy(() -> projectAuthService.assertMember(PROJECT_ID, nonMember))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyLong(), anyLong());
  }
}
