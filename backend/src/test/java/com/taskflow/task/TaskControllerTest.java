package com.taskflow.task;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskflow.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** MockMvc integration tests for the task CRUD endpoints. */
class TaskControllerTest extends IntegrationTestBase {

  @Test
  void createTask_asMemberTitleOnly_returns201WithDefaults() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Tasked");

    mockMvc
        .perform(
            post(tasksUrl(projectId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"First task\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.projectId").value(projectId))
        .andExpect(jsonPath("$.title").value("First task"))
        .andExpect(jsonPath("$.status").value("TODO"))
        .andExpect(jsonPath("$.priority").value("MEDIUM"))
        .andExpect(jsonPath("$.assignee").value((Object) null))
        .andExpect(jsonPath("$.createdBy.userId").value(owner.id()))
        .andExpect(jsonPath("$.createdAt").isNotEmpty());
  }

  @Test
  void createTask_withMemberAssignee_returns201WithAssignee() throws Exception {
    TestUser owner = registerUser();
    TestUser member = registerUser();
    long projectId = createProject(owner, "Assignable");
    addMember(owner, projectId, member);

    mockMvc
        .perform(
            post(tasksUrl(projectId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Assigned","priority":"HIGH","dueDate":"2026-03-01","assigneeId":%d}
                    """
                        .formatted(member.id())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.priority").value("HIGH"))
        .andExpect(jsonPath("$.dueDate").value("2026-03-01"))
        .andExpect(jsonPath("$.assignee.userId").value(member.id()));
  }

  @Test
  void createTask_assigneeNotMember_returns400() throws Exception {
    TestUser owner = registerUser();
    TestUser outsider = registerUser();
    long projectId = createProject(owner, "Closed");

    mockMvc
        .perform(
            post(tasksUrl(projectId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Bad assignee","assigneeId":%d}
                    """
                        .formatted(outsider.id())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ASSIGNEE_NOT_MEMBER"));
  }

  @Test
  void createTask_blankTitle_returns400() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Validated");

    mockMvc
        .perform(
            post(tasksUrl(projectId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')]").exists());
  }

  @Test
  void createTask_asNonMember_returns403() throws Exception {
    TestUser owner = registerUser();
    TestUser outsider = registerUser();
    long projectId = createProject(owner, "Private");

    mockMvc
        .perform(
            post(tasksUrl(projectId))
                .header("Authorization", bearer(outsider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Intruder\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void createTask_unknownProject_returns404() throws Exception {
    TestUser user = registerUser();

    mockMvc
        .perform(
            post(tasksUrl(999999999L))
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Nowhere\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
  }

  @Test
  void createTask_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            post(tasksUrl(1L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"No auth\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void listTasks_asMember_returnsProjectTasks() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Listed");
    createTask(owner, projectId, "One");
    createTask(owner, projectId, "Two");

    mockMvc
        .perform(get(tasksUrl(projectId)).header("Authorization", bearer(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].title").value("One"))
        .andExpect(jsonPath("$[1].title").value("Two"));
  }

  @Test
  void listTasks_filterByStatus_returnsMatching() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Filtered");
    long todoTask = createTask(owner, projectId, "Stays todo");
    long movedTask = createTask(owner, projectId, "Moves");
    patchStatus(owner, projectId, movedTask, "DONE");

    mockMvc
        .perform(
            get(tasksUrl(projectId)).param("status", "DONE").header("Authorization", bearer(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(movedTask));

    mockMvc
        .perform(
            get(tasksUrl(projectId)).param("status", "TODO").header("Authorization", bearer(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(todoTask));
  }

  @Test
  void listTasks_asNonMember_returns403() throws Exception {
    TestUser owner = registerUser();
    TestUser outsider = registerUser();
    long projectId = createProject(owner, "Hidden");

    mockMvc
        .perform(get(tasksUrl(projectId)).header("Authorization", bearer(outsider)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void getTask_asMember_returns200() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Readable");
    long taskId = createTask(owner, projectId, "Readable task");

    mockMvc
        .perform(get(taskUrl(projectId, taskId)).header("Authorization", bearer(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(taskId))
        .andExpect(jsonPath("$.title").value("Readable task"));
  }

  @Test
  void getTask_belongingToAnotherProject_returns404() throws Exception {
    TestUser owner = registerUser();
    long projectA = createProject(owner, "Project A");
    long projectB = createProject(owner, "Project B");
    long taskInB = createTask(owner, projectB, "In B");

    mockMvc
        .perform(get(taskUrl(projectA, taskInB)).header("Authorization", bearer(owner)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
  }

  @Test
  void getTask_asNonMember_returns403() throws Exception {
    TestUser owner = registerUser();
    TestUser outsider = registerUser();
    long projectId = createProject(owner, "Guarded");
    long taskId = createTask(owner, projectId, "Secret");

    mockMvc
        .perform(get(taskUrl(projectId, taskId)).header("Authorization", bearer(outsider)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void updateTask_asMember_returns200WithReplacedFields() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Editable");
    long taskId = createTask(owner, projectId, "Before");

    mockMvc
        .perform(
            put(taskUrl(projectId, taskId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"After","description":"Now with detail","status":"IN_PROGRESS",\
                    "priority":"LOW","dueDate":"2026-04-01","assigneeId":null}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("After"))
        .andExpect(jsonPath("$.description").value("Now with detail"))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.priority").value("LOW"))
        .andExpect(jsonPath("$.dueDate").value("2026-04-01"));
  }

  @Test
  void updateTask_missingStatus_returns400() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Bad update");
    long taskId = createTask(owner, projectId, "Task");

    mockMvc
        .perform(
            put(taskUrl(projectId, taskId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"No status\",\"priority\":\"LOW\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[?(@.field == 'status')]").exists());
  }

  @Test
  void updateStatus_asMember_returns200() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Board");
    long taskId = createTask(owner, projectId, "Drag me");

    mockMvc
        .perform(
            patch(taskUrl(projectId, taskId) + "/status")
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DONE"));
  }

  @Test
  void updateTask_asNonMember_returns403() throws Exception {
    TestUser owner = registerUser();
    TestUser outsider = registerUser();
    long projectId = createProject(owner, "Edit guarded");
    long taskId = createTask(owner, projectId, "Keep");

    mockMvc
        .perform(
            put(taskUrl(projectId, taskId))
                .header("Authorization", bearer(outsider))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Hijacked","description":null,"status":"DONE",\
                    "priority":"HIGH","dueDate":null,"assigneeId":null}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void updateStatus_asNonMember_returns403() throws Exception {
    TestUser owner = registerUser();
    TestUser outsider = registerUser();
    long projectId = createProject(owner, "Board guarded");
    long taskId = createTask(owner, projectId, "Drag me");

    mockMvc
        .perform(
            patch(taskUrl(projectId, taskId) + "/status")
                .header("Authorization", bearer(outsider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void updateStatus_missingStatus_returns400() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Bad status");
    long taskId = createTask(owner, projectId, "Task");

    mockMvc
        .perform(
            patch(taskUrl(projectId, taskId) + "/status")
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[?(@.field == 'status')]").exists());
  }

  @Test
  void deleteTask_asMember_returns204() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Deletable");
    long taskId = createTask(owner, projectId, "To delete");

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                    taskUrl(projectId, taskId))
                .header("Authorization", bearer(owner)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get(taskUrl(projectId, taskId)).header("Authorization", bearer(owner)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
  }

  @Test
  void deleteTask_asNonMember_returns403() throws Exception {
    TestUser owner = registerUser();
    TestUser outsider = registerUser();
    long projectId = createProject(owner, "Protected");
    long taskId = createTask(owner, projectId, "Keep");

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                    taskUrl(projectId, taskId))
                .header("Authorization", bearer(outsider)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  private void patchStatus(TestUser user, long projectId, long taskId, String status)
      throws Exception {
    mockMvc
        .perform(
            patch(taskUrl(projectId, taskId) + "/status")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"%s\"}".formatted(status)))
        .andExpect(status().isOk());
  }

  private static String tasksUrl(long projectId) {
    return "/api/v1/projects/" + projectId + "/tasks";
  }

  private static String taskUrl(long projectId, long taskId) {
    return tasksUrl(projectId) + "/" + taskId;
  }
}
