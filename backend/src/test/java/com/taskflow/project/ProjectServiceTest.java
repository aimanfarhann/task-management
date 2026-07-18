package com.taskflow.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.taskflow.common.exception.ForbiddenException;
import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.project.dto.CreateProjectRequest;
import com.taskflow.project.dto.ProjectDto;
import com.taskflow.project.dto.UpdateProjectRequest;
import com.taskflow.testutil.TestData;
import com.taskflow.user.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for {@link ProjectService} business rules. */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectMemberRepository projectMemberRepository;
  @Mock private ProjectAuthService projectAuthService;
  @Mock private com.taskflow.user.UserService userService;

  @InjectMocks private ProjectService projectService;

  @Test
  void listProjects_regularUser_returnsMembershipProjectsWithRolesAndCounts() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(7L);
    User user = TestData.user(7L, "user@test.com");
    Project ownedProject = TestData.project(1L, "Owned");
    Project joinedProject = TestData.project(2L, "Joined");
    when(projectMemberRepository.findByUserIdWithProject(7L))
        .thenReturn(
            List.of(
                TestData.membership(ownedProject, user, ProjectRole.OWNER),
                TestData.membership(joinedProject, user, ProjectRole.MEMBER)));
    when(projectMemberRepository.countByProjectIds(List.of(1L, 2L)))
        .thenReturn(List.of(TestData.memberCount(1L, 1), TestData.memberCount(2L, 3)));

    // Act
    List<ProjectDto> projects = projectService.listProjects(caller);

    // Assert
    assertThat(projects).hasSize(2);
    assertThat(projects.get(0).id()).isEqualTo(1L);
    assertThat(projects.get(0).myRole()).isEqualTo(ProjectRole.OWNER);
    assertThat(projects.get(0).memberCount()).isEqualTo(1);
    assertThat(projects.get(1).id()).isEqualTo(2L);
    assertThat(projects.get(1).myRole()).isEqualTo(ProjectRole.MEMBER);
    assertThat(projects.get(1).memberCount()).isEqualTo(3);
    verifyNoInteractions(projectRepository);
  }

  @Test
  void listProjects_admin_returnsAllProjectsReportingOwnerWhereNotMember() {
    // Arrange
    AuthenticatedUser admin = TestData.authAdmin(9L);
    User adminUser = TestData.admin(9L, "admin@test.com");
    Project memberProject = TestData.project(1L, "Member of");
    Project foreignProject = TestData.project(2L, "Not member");
    when(projectMemberRepository.findByUserIdWithProject(9L))
        .thenReturn(List.of(TestData.membership(memberProject, adminUser, ProjectRole.MEMBER)));
    when(projectRepository.findAllByOrderByIdAsc())
        .thenReturn(List.of(memberProject, foreignProject));
    when(projectMemberRepository.countByProjectIds(List.of(1L, 2L)))
        .thenReturn(List.of(TestData.memberCount(1L, 2), TestData.memberCount(2L, 5)));

    // Act
    List<ProjectDto> projects = projectService.listProjects(admin);

    // Assert: actual membership role wins; non-membership reports owner-level ability.
    assertThat(projects).hasSize(2);
    assertThat(projects.get(0).myRole()).isEqualTo(ProjectRole.MEMBER);
    assertThat(projects.get(1).myRole()).isEqualTo(ProjectRole.OWNER);
    assertThat(projects.get(1).memberCount()).isEqualTo(5);
  }

  @Test
  void createProject_valid_savesProjectAndOwnerMembership() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(7L);
    User creator = TestData.user(7L, "creator@test.com");
    when(projectRepository.save(any(Project.class)))
        .thenAnswer(
            invocation -> {
              Project saved = invocation.getArgument(0);
              ReflectionTestUtils.setField(saved, "id", 42L);
              return saved;
            });
    when(userService.findById(7L)).thenReturn(Optional.of(creator));
    when(projectMemberRepository.save(any(ProjectMember.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    ProjectDto created =
        projectService.createProject(
            new CreateProjectRequest("New Project", "About it", "teal"), caller);

    // Assert
    assertThat(created.id()).isEqualTo(42L);
    assertThat(created.name()).isEqualTo("New Project");
    assertThat(created.colorTag()).isEqualTo("teal");
    assertThat(created.archived()).isFalse();
    assertThat(created.myRole()).isEqualTo(ProjectRole.OWNER);
    assertThat(created.memberCount()).isEqualTo(1);
    ArgumentCaptor<ProjectMember> membership = ArgumentCaptor.forClass(ProjectMember.class);
    verify(projectMemberRepository).save(membership.capture());
    assertThat(membership.getValue().getProjectRole()).isEqualTo(ProjectRole.OWNER);
    assertThat(membership.getValue().getUser().getId()).isEqualTo(7L);
  }

  @Test
  void getProject_member_assertsMembershipBeforeLoading() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(7L);
    User user = TestData.user(7L, "user@test.com");
    Project project = TestData.project(1L, "Project");
    when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
    when(projectMemberRepository.findByProjectIdAndUserId(1L, 7L))
        .thenReturn(Optional.of(TestData.membership(project, user, ProjectRole.MEMBER)));
    when(projectMemberRepository.countByProjectId(1L)).thenReturn(4L);

    // Act
    ProjectDto dto = projectService.getProject(1L, caller);

    // Assert
    assertThat(dto.myRole()).isEqualTo(ProjectRole.MEMBER);
    assertThat(dto.memberCount()).isEqualTo(4);
    InOrder order = inOrder(projectAuthService, projectRepository);
    order.verify(projectAuthService).assertMember(1L, caller);
    order.verify(projectRepository).findById(1L);
  }

  @Test
  void getProject_nonMember_propagatesForbiddenWithoutTouchingRepositories() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(7L);
    doThrow(new ForbiddenException("not a member"))
        .when(projectAuthService)
        .assertMember(1L, caller);

    // Act + Assert
    assertThatThrownBy(() -> projectService.getProject(1L, caller))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(projectRepository);
    verifyNoInteractions(projectMemberRepository);
  }

  @Test
  void updateProject_owner_replacesAllMutableFields() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(7L);
    User user = TestData.user(7L, "user@test.com");
    Project project = TestData.project(1L, "Old Name");
    when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
    when(projectMemberRepository.findByProjectIdAndUserId(1L, 7L))
        .thenReturn(Optional.of(TestData.membership(project, user, ProjectRole.OWNER)));
    when(projectMemberRepository.countByProjectId(1L)).thenReturn(2L);

    // Act
    ProjectDto updated =
        projectService.updateProject(
            1L, new UpdateProjectRequest("New Name", "New description", "red", true), caller);

    // Assert
    assertThat(project.getName()).isEqualTo("New Name");
    assertThat(project.getDescription()).isEqualTo("New description");
    assertThat(project.getColorTag()).isEqualTo("red");
    assertThat(project.isArchived()).isTrue();
    assertThat(updated.archived()).isTrue();
    verify(projectAuthService).assertOwner(1L, caller);
  }

  @Test
  void updateProject_nonOwner_propagatesForbiddenWithoutTouchingRepositories() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(7L);
    doThrow(new ForbiddenException("not an owner"))
        .when(projectAuthService)
        .assertOwner(1L, caller);

    // Act + Assert
    assertThatThrownBy(
            () ->
                projectService.updateProject(
                    1L, new UpdateProjectRequest("Name", null, "red", false), caller))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(projectRepository);
  }
}
