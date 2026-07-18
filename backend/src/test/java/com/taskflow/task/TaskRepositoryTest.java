package com.taskflow.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskflow.IntegrationTestBase;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectRepository;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Repository tests for {@link TaskRepository} against Testcontainers PostgreSQL. */
class TaskRepositoryTest extends IntegrationTestBase {

  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private TaskRepository taskRepository;

  @Test
  void findByProjectFiltered_noFilters_returnsAllOrderedAndJoinFetched() {
    User creator = newUser();
    User assignee = newUser();
    Project project = projectRepository.save(new Project("Filter", null, "blue"));
    Task first = saveTask(project, creator, assignee, TaskPriority.HIGH);
    Task second = saveTask(project, creator, null, TaskPriority.LOW);

    List<Task> tasks = taskRepository.findByProjectFiltered(project.getId(), null, null, null);

    assertThat(tasks).extracting(Task::getId).containsExactly(first.getId(), second.getId());
    // Reading associations outside a transaction proves they were join-fetched.
    assertThat(tasks.get(0).getCreatedBy().getEmail()).isEqualTo(creator.getEmail());
    assertThat(tasks.get(0).getAssignee().getEmail()).isEqualTo(assignee.getEmail());
    assertThat(tasks.get(1).getAssignee()).isNull();
  }

  @Test
  void findByProjectFiltered_statusFilter_returnsOnlyMatchingStatus() {
    User creator = newUser();
    Project project = projectRepository.save(new Project("Statuses", null, "teal"));
    saveTask(project, creator, null, TaskPriority.MEDIUM);
    Task inProgress = saveTask(project, creator, null, TaskPriority.MEDIUM);
    inProgress.changeStatus(TaskStatus.IN_PROGRESS);
    taskRepository.saveAndFlush(inProgress);

    List<Task> matches =
        taskRepository.findByProjectFiltered(project.getId(), TaskStatus.IN_PROGRESS, null, null);

    assertThat(matches).extracting(Task::getId).containsExactly(inProgress.getId());
  }

  @Test
  void findByProjectFiltered_assigneeFilter_returnsOnlyThatAssignee() {
    User creator = newUser();
    User assignee = newUser();
    Project project = projectRepository.save(new Project("Assignees", null, "green"));
    Task assigned = saveTask(project, creator, assignee, TaskPriority.MEDIUM);
    saveTask(project, creator, null, TaskPriority.MEDIUM);

    List<Task> matches =
        taskRepository.findByProjectFiltered(project.getId(), null, null, assignee.getId());

    assertThat(matches).extracting(Task::getId).containsExactly(assigned.getId());
  }

  @Test
  void findAssignedInProjects_returnsOnlyTheUsersTasksWithinTheProjects() {
    User creator = newUser();
    User assignee = newUser();
    Project mine = projectRepository.save(new Project("Mine", null, "indigo"));
    Project other = projectRepository.save(new Project("Other", null, "amber"));
    Task assignedInMine = saveTask(mine, creator, assignee, TaskPriority.MEDIUM);
    saveTask(mine, creator, null, TaskPriority.MEDIUM); // unassigned
    saveTask(other, creator, assignee, TaskPriority.MEDIUM); // outside the project set

    List<Task> assigned =
        taskRepository.findAssignedInProjects(assignee.getId(), List.of(mine.getId()));

    assertThat(assigned).extracting(Task::getId).containsExactly(assignedInMine.getId());
  }

  @Test
  void countByStatusInProjects_groupsByProjectAndStatus() {
    User creator = newUser();
    Project project = projectRepository.save(new Project("Counts", null, "purple"));
    saveTask(project, creator, null, TaskPriority.MEDIUM); // TODO
    saveTask(project, creator, null, TaskPriority.MEDIUM); // TODO
    Task done = saveTask(project, creator, null, TaskPriority.MEDIUM);
    done.changeStatus(TaskStatus.DONE);
    taskRepository.saveAndFlush(done);

    Map<TaskStatus, Long> counts =
        taskRepository.countByStatusInProjects(List.of(project.getId())).stream()
            .collect(Collectors.toMap(TaskStatusCount::getStatus, TaskStatusCount::getCount));

    assertThat(counts).containsEntry(TaskStatus.TODO, 2L).containsEntry(TaskStatus.DONE, 1L);
  }

  private Task saveTask(Project project, User creator, User assignee, TaskPriority priority) {
    return taskRepository.save(new Task(project, "Task", null, priority, null, assignee, creator));
  }

  private User newUser() {
    return userRepository.save(
        new User("task-repo-" + UUID.randomUUID() + "@test.com", "$2a$10$hash", "Repo User"));
  }
}
