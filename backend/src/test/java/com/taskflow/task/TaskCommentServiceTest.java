package com.taskflow.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskflow.common.exception.ForbiddenException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectAuthService;
import com.taskflow.task.dto.CommentDto;
import com.taskflow.task.dto.CreateCommentRequest;
import com.taskflow.testutil.TestData;
import com.taskflow.user.User;
import com.taskflow.user.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for {@link TaskCommentService} business rules, chiefly delete authorization. */
@ExtendWith(MockitoExtension.class)
class TaskCommentServiceTest {

  private static final Long PROJECT_ID = 1L;
  private static final Long TASK_ID = 5L;
  private static final Long COMMENT_ID = 9L;
  private static final Long CALLER_ID = 7L;
  private static final Long OTHER_ID = 8L;

  @Mock private TaskCommentRepository taskCommentRepository;
  @Mock private TaskService taskService;
  @Mock private ProjectAuthService projectAuthService;
  @Mock private UserService userService;

  @InjectMocks private TaskCommentService taskCommentService;

  @Test
  void createComment_member_savesWithCallerAsAuthor() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    Task task = task();
    User author = TestData.user(CALLER_ID, "author@test.com");
    when(taskService.requireTask(PROJECT_ID, TASK_ID)).thenReturn(task);
    when(userService.findById(CALLER_ID)).thenReturn(Optional.of(author));
    when(taskCommentRepository.save(any(TaskComment.class)))
        .thenAnswer(
            invocation -> {
              TaskComment saved = invocation.getArgument(0);
              ReflectionTestUtils.setField(saved, "id", COMMENT_ID);
              return saved;
            });

    // Act
    CommentDto dto =
        taskCommentService.createComment(
            PROJECT_ID, TASK_ID, new CreateCommentRequest("Looks good"), caller);

    // Assert
    assertThat(dto.id()).isEqualTo(COMMENT_ID);
    assertThat(dto.taskId()).isEqualTo(TASK_ID);
    assertThat(dto.body()).isEqualTo("Looks good");
    assertThat(dto.author().userId()).isEqualTo(CALLER_ID);
    verify(projectAuthService).assertMember(PROJECT_ID, caller);
  }

  @Test
  void listComments_member_assertsMembershipAndTaskThenMaps() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    Task task = task();
    User author = TestData.user(CALLER_ID, "author@test.com");
    when(taskCommentRepository.findByTaskIdWithAuthor(TASK_ID))
        .thenReturn(List.of(TestData.comment(COMMENT_ID, task, author, "Hello")));

    // Act
    List<CommentDto> comments = taskCommentService.listComments(PROJECT_ID, TASK_ID, caller);

    // Assert
    assertThat(comments).hasSize(1);
    assertThat(comments.get(0).body()).isEqualTo("Hello");
    verify(projectAuthService).assertMember(PROJECT_ID, caller);
    verify(taskService).requireTask(PROJECT_ID, TASK_ID);
  }

  @Test
  void deleteComment_asAuthor_deletesWithoutOwnerCheck() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    TaskComment comment =
        TestData.comment(COMMENT_ID, task(), TestData.user(CALLER_ID, "author@test.com"), "Mine");
    when(taskCommentRepository.findByIdWithAuthor(COMMENT_ID)).thenReturn(Optional.of(comment));

    // Act
    taskCommentService.deleteComment(PROJECT_ID, TASK_ID, COMMENT_ID, caller);

    // Assert
    verify(taskCommentRepository).delete(comment);
    verify(projectAuthService, never()).assertOwner(any(), any());
  }

  @Test
  void deleteComment_asOwnerNotAuthor_deletes() {
    // Arrange: caller is not the author, but assertOwner passes (owner or admin).
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    TaskComment comment =
        TestData.comment(
            COMMENT_ID, task(), TestData.user(OTHER_ID, "other@test.com"), "Someone else's");
    when(taskCommentRepository.findByIdWithAuthor(COMMENT_ID)).thenReturn(Optional.of(comment));

    // Act
    taskCommentService.deleteComment(PROJECT_ID, TASK_ID, COMMENT_ID, caller);

    // Assert
    verify(projectAuthService).assertOwner(PROJECT_ID, caller);
    verify(taskCommentRepository).delete(comment);
  }

  @Test
  void deleteComment_nonAuthorNonOwner_throwsForbiddenWithoutDeleting() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    TaskComment comment =
        TestData.comment(
            COMMENT_ID, task(), TestData.user(OTHER_ID, "other@test.com"), "Someone else's");
    when(taskCommentRepository.findByIdWithAuthor(COMMENT_ID)).thenReturn(Optional.of(comment));
    org.mockito.Mockito.doThrow(new ForbiddenException("not an owner"))
        .when(projectAuthService)
        .assertOwner(PROJECT_ID, caller);

    // Act + Assert
    assertThatThrownBy(
            () -> taskCommentService.deleteComment(PROJECT_ID, TASK_ID, COMMENT_ID, caller))
        .isInstanceOf(ForbiddenException.class);
    verify(taskCommentRepository, never()).delete(any());
  }

  @Test
  void deleteComment_unknownComment_throwsCommentNotFound() {
    // Arrange
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    when(taskCommentRepository.findByIdWithAuthor(COMMENT_ID)).thenReturn(Optional.empty());

    // Act + Assert
    assertThatThrownBy(
            () -> taskCommentService.deleteComment(PROJECT_ID, TASK_ID, COMMENT_ID, caller))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("COMMENT_NOT_FOUND"));
  }

  @Test
  void deleteComment_commentBelongsToAnotherTask_throwsCommentNotFound() {
    // Arrange: the comment exists but hangs off a different task than the path task.
    AuthenticatedUser caller = TestData.authUser(CALLER_ID);
    Project project = TestData.project(PROJECT_ID, "Project");
    User creator = TestData.user(CALLER_ID, "creator@test.com");
    Task otherTask = TestData.task(999L, project, creator);
    TaskComment comment =
        TestData.comment(
            COMMENT_ID, otherTask, TestData.user(CALLER_ID, "author@test.com"), "Elsewhere");
    when(taskCommentRepository.findByIdWithAuthor(COMMENT_ID)).thenReturn(Optional.of(comment));

    // Act + Assert
    assertThatThrownBy(
            () -> taskCommentService.deleteComment(PROJECT_ID, TASK_ID, COMMENT_ID, caller))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("COMMENT_NOT_FOUND"));
    verify(taskCommentRepository, never()).delete(any());
  }

  private Task task() {
    Project project = TestData.project(PROJECT_ID, "Project");
    User creator = TestData.user(CALLER_ID, "creator@test.com");
    return TestData.task(TASK_ID, project, creator);
  }
}
