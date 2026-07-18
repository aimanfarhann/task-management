package com.taskflow.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskflow.IntegrationTestBase;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Repository tests for {@link ProjectMemberRepository} against Testcontainers PostgreSQL. */
class ProjectMemberRepositoryTest extends IntegrationTestBase {

  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;

  @Test
  void findByProjectIdWithUser_joinFetchesUsers_readableWithoutSession() {
    User owner = newUser();
    User member = newUser();
    Project project = projectRepository.save(new Project("Fetch test", null, "teal"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));
    projectMemberRepository.save(new ProjectMember(project, member, ProjectRole.MEMBER));

    List<ProjectMember> members = projectMemberRepository.findByProjectIdWithUser(project.getId());

    // Accessing user fields outside a transaction proves the association was join-fetched.
    assertThat(members).hasSize(2);
    assertThat(members)
        .extracting(m -> m.getUser().getEmail())
        .containsExactlyInAnyOrder(owner.getEmail(), member.getEmail());
  }

  @Test
  void findByUserIdWithProject_returnsOnlyThatUsersMemberships() {
    User user = newUser();
    User other = newUser();
    Project mine = projectRepository.save(new Project("Mine", null, "red"));
    Project theirs = projectRepository.save(new Project("Theirs", null, "green"));
    projectMemberRepository.save(new ProjectMember(mine, user, ProjectRole.OWNER));
    projectMemberRepository.save(new ProjectMember(theirs, other, ProjectRole.OWNER));

    List<ProjectMember> memberships = projectMemberRepository.findByUserIdWithProject(user.getId());

    assertThat(memberships).hasSize(1);
    assertThat(memberships.get(0).getProject().getName()).isEqualTo("Mine");
  }

  @Test
  void countByProjectIds_groupsCountsPerProject() {
    User first = newUser();
    User second = newUser();
    Project bigger = projectRepository.save(new Project("Bigger", null, "blue"));
    Project smaller = projectRepository.save(new Project("Smaller", null, "amber"));
    projectMemberRepository.save(new ProjectMember(bigger, first, ProjectRole.OWNER));
    projectMemberRepository.save(new ProjectMember(bigger, second, ProjectRole.MEMBER));
    projectMemberRepository.save(new ProjectMember(smaller, first, ProjectRole.OWNER));

    Map<Long, Long> counts =
        projectMemberRepository.countByProjectIds(List.of(bigger.getId(), smaller.getId())).stream()
            .collect(
                Collectors.toMap(
                    ProjectMemberCount::getProjectId, ProjectMemberCount::getMemberCount));

    assertThat(counts).containsEntry(bigger.getId(), 2L).containsEntry(smaller.getId(), 1L);
  }

  @Test
  void countByProjectIdAndProjectRole_countsOnlyThatRole() {
    User owner = newUser();
    User member = newUser();
    Project project = projectRepository.save(new Project("Role counts", null, "indigo"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));
    projectMemberRepository.save(new ProjectMember(project, member, ProjectRole.MEMBER));

    assertThat(
            projectMemberRepository.countByProjectIdAndProjectRole(
                project.getId(), ProjectRole.OWNER))
        .isEqualTo(1);
    assertThat(
            projectMemberRepository.countByProjectIdAndProjectRole(
                project.getId(), ProjectRole.MEMBER))
        .isEqualTo(1);
  }

  private User newUser() {
    return userRepository.save(
        new User("member-repo-" + UUID.randomUUID() + "@test.com", "$2a$10$hash", "Repo User"));
  }
}
