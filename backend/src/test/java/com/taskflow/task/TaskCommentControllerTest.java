package com.taskflow.task;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskflow.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** MockMvc integration tests for the task comment endpoints. */
class TaskCommentControllerTest extends IntegrationTestBase {

  @Test
  void createComment_asMember_returns201WithAuthor() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Commented");
    long taskId = createTask(owner, projectId, "Discussed");

    mockMvc
        .perform(
            post(commentsUrl(projectId, taskId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"First comment\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.taskId").value(taskId))
        .andExpect(jsonPath("$.body").value("First comment"))
        .andExpect(jsonPath("$.author.userId").value(owner.id()))
        .andExpect(jsonPath("$.createdAt").isNotEmpty());
  }

  @Test
  void createComment_blankBody_returns400() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Validated");
    long taskId = createTask(owner, projectId, "Task");

    mockMvc
        .perform(
            post(commentsUrl(projectId, taskId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[?(@.field == 'body')]").exists());
  }

  @Test
  void createComment_asNonMember_returns403() throws Exception {
    TestUser owner = registerUser();
    TestUser outsider = registerUser();
    long projectId = createProject(owner, "Closed");
    long taskId = createTask(owner, projectId, "Task");

    mockMvc
        .perform(
            post(commentsUrl(projectId, taskId))
                .header("Authorization", bearer(outsider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Intrusion\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void createComment_unknownTask_returns404() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "No task");

    mockMvc
        .perform(
            post(commentsUrl(projectId, 999999999L))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Ghost\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
  }

  @Test
  void comments_taskBelongingToAnotherProject_returns404() throws Exception {
    TestUser owner = registerUser();
    long projectA = createProject(owner, "Project A");
    long projectB = createProject(owner, "Project B");
    long taskInB = createTask(owner, projectB, "In B");

    // Reaching project B's task through project A's comment routes must 404, never leak.
    mockMvc
        .perform(get(commentsUrl(projectA, taskInB)).header("Authorization", bearer(owner)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));

    mockMvc
        .perform(
            post(commentsUrl(projectA, taskInB))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Leak?\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
  }

  @Test
  void listComments_returnsChronologicalOrder() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Threaded");
    long taskId = createTask(owner, projectId, "Task");
    createComment(owner, projectId, taskId, "Earliest");
    createComment(owner, projectId, taskId, "Latest");

    mockMvc
        .perform(get(commentsUrl(projectId, taskId)).header("Authorization", bearer(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].body").value("Earliest"))
        .andExpect(jsonPath("$[1].body").value("Latest"));
  }

  @Test
  void listComments_asNonMember_returns403() throws Exception {
    TestUser owner = registerUser();
    TestUser outsider = registerUser();
    long projectId = createProject(owner, "Private");
    long taskId = createTask(owner, projectId, "Task");

    mockMvc
        .perform(get(commentsUrl(projectId, taskId)).header("Authorization", bearer(outsider)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void deleteComment_asAuthor_returns204() throws Exception {
    TestUser owner = registerUser();
    TestUser member = registerUser();
    long projectId = createProject(owner, "Authored");
    addMember(owner, projectId, member);
    long taskId = createTask(owner, projectId, "Task");
    long commentId = createComment(member, projectId, taskId, "My comment");

    mockMvc
        .perform(
            delete(commentUrl(projectId, taskId, commentId))
                .header("Authorization", bearer(member)))
        .andExpect(status().isNoContent());
  }

  @Test
  void deleteComment_asProjectOwnerNotAuthor_returns204() throws Exception {
    TestUser owner = registerUser();
    TestUser member = registerUser();
    long projectId = createProject(owner, "Owner moderated");
    addMember(owner, projectId, member);
    long taskId = createTask(owner, projectId, "Task");
    long commentId = createComment(member, projectId, taskId, "Member's comment");

    mockMvc
        .perform(
            delete(commentUrl(projectId, taskId, commentId)).header("Authorization", bearer(owner)))
        .andExpect(status().isNoContent());
  }

  @Test
  void deleteComment_asOtherMemberNotAuthor_returns403() throws Exception {
    TestUser owner = registerUser();
    TestUser author = registerUser();
    TestUser other = registerUser();
    long projectId = createProject(owner, "Peer protected");
    addMember(owner, projectId, author);
    addMember(owner, projectId, other);
    long taskId = createTask(owner, projectId, "Task");
    long commentId = createComment(author, projectId, taskId, "Author's comment");

    mockMvc
        .perform(
            delete(commentUrl(projectId, taskId, commentId)).header("Authorization", bearer(other)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void deleteComment_unknownComment_returns404() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "No comment");
    long taskId = createTask(owner, projectId, "Task");

    mockMvc
        .perform(
            delete(commentUrl(projectId, taskId, 999999999L))
                .header("Authorization", bearer(owner)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
  }

  private long createComment(TestUser user, long projectId, long taskId, String body)
      throws Exception {
    String response =
        mockMvc
            .perform(
                post(commentsUrl(projectId, taskId))
                    .header("Authorization", bearer(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"body\":\"%s\"}".formatted(body)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("id").asLong();
  }

  private static String commentsUrl(long projectId, long taskId) {
    return "/api/v1/projects/" + projectId + "/tasks/" + taskId + "/comments";
  }

  private static String commentUrl(long projectId, long taskId, long commentId) {
    return commentsUrl(projectId, taskId) + "/" + commentId;
  }
}
