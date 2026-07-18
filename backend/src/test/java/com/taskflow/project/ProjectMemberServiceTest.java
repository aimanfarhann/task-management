package com.taskflow.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.taskflow.common.exception.ConflictException;
import com.taskflow.common.exception.ForbiddenException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.project.dto.AddMemberRequest;
import com.taskflow.project.dto.MemberDto;
import com.taskflow.testutil.TestData;
import com.taskflow.user.User;
import com.taskflow.user.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/** Unit tests for {@link ProjectMemberService} business rules. */
@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

  private static final Long PROJECT_ID = 10L;

  @Mock private ProjectMemberRepository projectMemberRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectAuthService projectAuthService;
  @Mock private UserService userService;

  @InjectMocks private ProjectMemberService projectMemberService;

  @Test
  void listMembers_member_returnsMappedMembers() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(1L);
    Project project = TestData.project(PROJECT_ID, "Project");
    User owner = TestData.user(1L, "owner@test.com");
    User member = TestData.user(2L, "member@test.com");
    when(projectMemberRepository.findByProjectIdWithUser(PROJECT_ID))
        .thenReturn(
            List.of(
                TestData.membership(project, owner, ProjectRole.OWNER),
                TestData.membership(project, member, ProjectRole.MEMBER)));

    // Act
    List<MemberDto> members = projectMemberService.listMembers(PROJECT_ID, caller);

    // Assert
    assertThat(members).hasSize(2);
    assertThat(members.get(0).userId()).isEqualTo(1L);
    assertThat(members.get(0).projectRole()).isEqualTo(ProjectRole.OWNER);
    assertThat(members.get(1).email()).isEqualTo("member@test.com");
    verify(projectAuthService).assertMember(PROJECT_ID, caller);
  }

  @Test
  void addMember_defaultRole_savesMembership() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(1L);
    Project project = TestData.project(PROJECT_ID, "Project");
    User invited = TestData.user(2L, "invited@test.com");
    when(userService.findByEmail("invited@test.com")).thenReturn(Optional.of(invited));
    when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 2L)).thenReturn(false);
    when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
    when(projectMemberRepository.saveAndFlush(any(ProjectMember.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    MemberDto added =
        projectMemberService.addMember(
            PROJECT_ID, new AddMemberRequest("invited@test.com", null), caller);

    // Assert: omitted role defaults to MEMBER.
    assertThat(added.userId()).isEqualTo(2L);
    assertThat(added.projectRole()).isEqualTo(ProjectRole.MEMBER);
    verify(projectAuthService).assertOwner(PROJECT_ID, caller);
  }

  @Test
  void addMember_explicitOwnerRole_savesOwnerMembership() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(1L);
    Project project = TestData.project(PROJECT_ID, "Project");
    User invited = TestData.user(2L, "invited@test.com");
    when(userService.findByEmail("invited@test.com")).thenReturn(Optional.of(invited));
    when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 2L)).thenReturn(false);
    when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
    when(projectMemberRepository.saveAndFlush(any(ProjectMember.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    MemberDto added =
        projectMemberService.addMember(
            PROJECT_ID, new AddMemberRequest("invited@test.com", ProjectRole.OWNER), caller);

    // Assert
    assertThat(added.projectRole()).isEqualTo(ProjectRole.OWNER);
    ArgumentCaptor<ProjectMember> saved = ArgumentCaptor.forClass(ProjectMember.class);
    verify(projectMemberRepository).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getProjectRole()).isEqualTo(ProjectRole.OWNER);
  }

  @Test
  void addMember_unknownEmail_throwsUserNotFound() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(1L);
    when(userService.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

    // Act + Assert
    assertThatThrownBy(
            () ->
                projectMemberService.addMember(
                    PROJECT_ID, new AddMemberRequest("ghost@test.com", null), caller))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("USER_NOT_FOUND"));
    verify(projectMemberRepository, never()).saveAndFlush(any());
  }

  @Test
  void addMember_existingMember_throwsAlreadyMember() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(1L);
    User invited = TestData.user(2L, "invited@test.com");
    when(userService.findByEmail("invited@test.com")).thenReturn(Optional.of(invited));
    when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 2L)).thenReturn(true);

    // Act + Assert
    assertThatThrownBy(
            () ->
                projectMemberService.addMember(
                    PROJECT_ID, new AddMemberRequest("invited@test.com", null), caller))
        .isInstanceOfSatisfying(
            ConflictException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("ALREADY_MEMBER"));
    verify(projectMemberRepository, never()).saveAndFlush(any());
  }

  @Test
  void addMember_concurrentDuplicate_translatesConstraintViolationToConflict() {
    // Arrange: the membership check passes, but a racing add already inserted the row, so the
    // composite primary key rejects this one at flush time.
    AuthenticatedUser caller = TestData.authUser(1L);
    Project project = TestData.project(PROJECT_ID, "Project");
    User invited = TestData.user(2L, "invited@test.com");
    when(userService.findByEmail("invited@test.com")).thenReturn(Optional.of(invited));
    when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, 2L)).thenReturn(false);
    when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
    when(projectMemberRepository.saveAndFlush(any(ProjectMember.class)))
        .thenThrow(new DataIntegrityViolationException("composite primary key violation"));

    // Act + Assert
    assertThatThrownBy(
            () ->
                projectMemberService.addMember(
                    PROJECT_ID, new AddMemberRequest("invited@test.com", null), caller))
        .isInstanceOfSatisfying(
            ConflictException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("ALREADY_MEMBER"));
  }

  @Test
  void addMember_nonOwner_propagatesForbiddenBeforeAnyLookup() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(1L);
    doThrow(new ForbiddenException("not an owner"))
        .when(projectAuthService)
        .assertOwner(PROJECT_ID, caller);

    // Act + Assert
    assertThatThrownBy(
            () ->
                projectMemberService.addMember(
                    PROJECT_ID, new AddMemberRequest("invited@test.com", null), caller))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(userService);
    verifyNoInteractions(projectMemberRepository);
  }

  @Test
  void removeMember_ownerRemovesOtherMember_deletesMembership() {
    // Arrange
    AuthenticatedUser owner = TestData.authUser(1L);
    ProjectMember target = memberMembership(2L, ProjectRole.MEMBER);
    when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, 2L))
        .thenReturn(Optional.of(target));

    // Act
    projectMemberService.removeMember(PROJECT_ID, 2L, owner);

    // Assert
    verify(projectAuthService).assertMember(PROJECT_ID, owner);
    verify(projectAuthService).assertOwner(PROJECT_ID, owner);
    verify(projectMemberRepository).delete(target);
  }

  @Test
  void removeMember_memberLeavesThemselves_deletesWithoutOwnerCheck() {
    // Arrange
    AuthenticatedUser member = TestData.authUser(2L);
    ProjectMember target = memberMembership(2L, ProjectRole.MEMBER);
    when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, 2L))
        .thenReturn(Optional.of(target));

    // Act
    projectMemberService.removeMember(PROJECT_ID, 2L, member);

    // Assert: leaving is allowed for plain members — no owner assertion.
    verify(projectAuthService, never()).assertOwner(anyLong(), any());
    verify(projectMemberRepository).delete(target);
  }

  @Test
  void removeMember_memberRemovingSomeoneElse_propagatesForbidden() {
    // Arrange
    AuthenticatedUser member = TestData.authUser(2L);
    doThrow(new ForbiddenException("not an owner"))
        .when(projectAuthService)
        .assertOwner(PROJECT_ID, member);

    // Act + Assert
    assertThatThrownBy(() -> projectMemberService.removeMember(PROJECT_ID, 3L, member))
        .isInstanceOf(ForbiddenException.class);
    verify(projectMemberRepository, never()).delete(any());
  }

  @Test
  void removeMember_lastOwner_throwsLastOwnerConflict() {
    // Arrange: the target is an OWNER and the only one.
    AuthenticatedUser owner = TestData.authUser(1L);
    ProjectMember target = memberMembership(1L, ProjectRole.OWNER);
    when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, 1L))
        .thenReturn(Optional.of(target));
    when(projectMemberRepository.countByProjectIdAndProjectRole(PROJECT_ID, ProjectRole.OWNER))
        .thenReturn(1L);

    // Act + Assert
    assertThatThrownBy(() -> projectMemberService.removeMember(PROJECT_ID, 1L, owner))
        .isInstanceOfSatisfying(
            ConflictException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("LAST_OWNER"));
    verify(projectMemberRepository, never()).delete(any());
  }

  @Test
  void removeMember_ownerWithCoOwner_deletesMembership() {
    // Arrange: two owners exist, so one may leave.
    AuthenticatedUser owner = TestData.authUser(1L);
    ProjectMember target = memberMembership(1L, ProjectRole.OWNER);
    when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, 1L))
        .thenReturn(Optional.of(target));
    when(projectMemberRepository.countByProjectIdAndProjectRole(PROJECT_ID, ProjectRole.OWNER))
        .thenReturn(2L);

    // Act
    projectMemberService.removeMember(PROJECT_ID, 1L, owner);

    // Assert
    verify(projectMemberRepository).delete(target);
  }

  @Test
  void removeMember_targetNotMember_throwsMemberNotFound() {
    // Arrange
    AuthenticatedUser owner = TestData.authUser(1L);
    when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, 5L))
        .thenReturn(Optional.empty());

    // Act + Assert
    assertThatThrownBy(() -> projectMemberService.removeMember(PROJECT_ID, 5L, owner))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("MEMBER_NOT_FOUND"));
  }

  private ProjectMember memberMembership(Long userId, ProjectRole role) {
    return TestData.membership(
        TestData.project(PROJECT_ID, "Project"),
        TestData.user(userId, "user-" + userId + "@test.com"),
        role);
  }
}
