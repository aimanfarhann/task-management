package com.taskflow.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskflow.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** MockMvc integration tests for the dashboard endpoint. */
class DashboardControllerTest extends IntegrationTestBase {

  @Test
  void getDashboard_returnsAssignedTasksAndPerProjectSummaries() throws Exception {
    TestUser user = registerUser();
    long alpha = createProject(user, "Alpha");
    long beta = createProject(user, "Beta"); // no tasks — summary must still appear with zeros
    long assignedTodo = createAssignedTask(user, alpha, "Mine todo", user.id());
    long assignedDone = createAssignedTask(user, alpha, "Mine done", user.id());
    patchStatus(user, alpha, assignedDone, "DONE");
    createTask(user, alpha, "Unassigned"); // counted in summary, absent from myTasks

    mockMvc
        .perform(get("/api/v1/dashboard").header("Authorization", bearer(user)))
        .andExpect(status().isOk())
        // Only the two tasks assigned to the caller, each carrying its project's name and color.
        .andExpect(jsonPath("$.myTasks.length()").value(2))
        .andExpect(jsonPath("$.myTasks[0].projectName").value("Alpha"))
        .andExpect(jsonPath("$.myTasks[0].projectColorTag").value("blue"))
        .andExpect(jsonPath("$.myTasks[0].id").value(assignedTodo))
        .andExpect(jsonPath("$.myTasks[1].id").value(assignedDone))
        // One summary per membership, ordered by project id; Alpha then Beta.
        .andExpect(jsonPath("$.projectSummaries.length()").value(2))
        .andExpect(jsonPath("$.projectSummaries[0].projectId").value(alpha))
        .andExpect(jsonPath("$.projectSummaries[0].projectName").value("Alpha"))
        .andExpect(jsonPath("$.projectSummaries[0].todoCount").value(2))
        .andExpect(jsonPath("$.projectSummaries[0].inProgressCount").value(0))
        .andExpect(jsonPath("$.projectSummaries[0].doneCount").value(1))
        .andExpect(jsonPath("$.projectSummaries[1].projectId").value(beta))
        .andExpect(jsonPath("$.projectSummaries[1].todoCount").value(0))
        .andExpect(jsonPath("$.projectSummaries[1].doneCount").value(0));
  }

  @Test
  void getDashboard_excludesOtherUsersProjectsAndAssignments() throws Exception {
    TestUser me = registerUser();
    TestUser other = registerUser();
    long mine = createProject(me, "Mine");
    createProject(other, "Theirs");
    createAssignedTask(me, mine, "My task", me.id());

    mockMvc
        .perform(get("/api/v1/dashboard").header("Authorization", bearer(me)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.myTasks.length()").value(1))
        .andExpect(jsonPath("$.myTasks[0].projectName").value("Mine"))
        .andExpect(jsonPath("$.projectSummaries.length()").value(1))
        .andExpect(jsonPath("$.projectSummaries[0].projectName").value("Mine"));
  }

  @Test
  void getDashboard_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/v1/dashboard"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  private long createAssignedTask(TestUser user, long projectId, String title, long assigneeId)
      throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/v1/projects/" + projectId + "/tasks")
                    .header("Authorization", bearer(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"title":"%s","assigneeId":%d}
                        """
                            .formatted(title, assigneeId)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("id").asLong();
  }

  private void patchStatus(TestUser user, long projectId, long taskId, String status)
      throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/projects/" + projectId + "/tasks/" + taskId + "/status")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"%s\"}".formatted(status)))
        .andExpect(status().isOk());
  }
}
