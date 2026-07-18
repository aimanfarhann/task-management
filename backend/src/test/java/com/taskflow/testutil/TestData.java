package com.taskflow.testutil;

import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectMember;
import com.taskflow.project.ProjectMemberCount;
import com.taskflow.project.ProjectRole;
import com.taskflow.task.Task;
import com.taskflow.task.TaskComment;
import com.taskflow.task.TaskPriority;
import com.taskflow.task.TaskStatus;
import com.taskflow.task.TaskStatusCount;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Builders for entity fixtures in unit tests. Ids are normally assigned by the database, so tests
 * set them via reflection.
 */
public final class TestData {

  private TestData() {}

  /** Creates an active USER entity with the given id. */
  public static User user(Long id, String email) {
    User user = new User(email, "$2a$10$fixture-hash", "Test User");
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  /** Creates an active ADMIN entity with the given id. */
  public static User admin(Long id, String email) {
    User user = user(id, email);
    ReflectionTestUtils.setField(user, "role", Role.ADMIN);
    return user;
  }

  /** Creates a deactivated USER entity with the given id. */
  public static User inactiveUser(Long id, String email) {
    User user = user(id, email);
    ReflectionTestUtils.setField(user, "active", false);
    return user;
  }

  /** Creates a project entity with the given id. */
  public static Project project(Long id, String name) {
    Project project = new Project(name, null, "blue");
    ReflectionTestUtils.setField(project, "id", id);
    return project;
  }

  /** Creates a membership joining the given user to the given project. */
  public static ProjectMember membership(Project project, User user, ProjectRole role) {
    return new ProjectMember(project, user, role);
  }

  /** Creates the authenticated principal for a regular user. */
  public static AuthenticatedUser authUser(Long id) {
    return new AuthenticatedUser(id, "user-" + id + "@test.com", Role.USER);
  }

  /** Creates the authenticated principal for a system admin. */
  public static AuthenticatedUser authAdmin(Long id) {
    return new AuthenticatedUser(id, "admin-" + id + "@test.com", Role.ADMIN);
  }

  /** Creates a task with the given id in {@code TODO}, MEDIUM priority, no assignee. */
  public static Task task(Long id, Project project, User createdBy) {
    Task task = new Task(project, "Task " + id, null, TaskPriority.MEDIUM, null, null, createdBy);
    ReflectionTestUtils.setField(task, "id", id);
    return task;
  }

  /** Creates a task with the given id, assignee, and status. */
  public static Task task(
      Long id, Project project, User createdBy, User assignee, TaskStatus status) {
    Task task =
        new Task(project, "Task " + id, null, TaskPriority.MEDIUM, null, assignee, createdBy);
    ReflectionTestUtils.setField(task, "id", id);
    ReflectionTestUtils.setField(task, "status", status);
    return task;
  }

  /** Creates a comment with the given id on the given task. */
  public static TaskComment comment(Long id, Task task, User author, String body) {
    TaskComment comment = new TaskComment(task, author, body);
    ReflectionTestUtils.setField(comment, "id", id);
    return comment;
  }

  /** Creates a status-count projection as returned by the group-by count query. */
  public static TaskStatusCount taskStatusCount(Long projectId, TaskStatus status, long count) {
    return new TaskStatusCount() {
      @Override
      public Long getProjectId() {
        return projectId;
      }

      @Override
      public TaskStatus getStatus() {
        return status;
      }

      @Override
      public long getCount() {
        return count;
      }
    };
  }

  /** Creates a member-count projection as returned by the group-by count query. */
  public static ProjectMemberCount memberCount(Long projectId, long count) {
    return new ProjectMemberCount() {
      @Override
      public Long getProjectId() {
        return projectId;
      }

      @Override
      public long getMemberCount() {
        return count;
      }
    };
  }
}
