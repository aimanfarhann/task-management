package com.taskflow.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskflow.IntegrationTestBase;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

/** MockMvc integration tests for the project CRUD endpoints. */
class ProjectControllerTest extends IntegrationTestBase {

  @Test
  void listProjects_memberOfProjects_returnsExactlyOwnProjects() throws Exception {
    TestUser userA = registerUser();
    TestUser userB = registerUser();
    long firstProject = createProject(userA, "A first");
    long secondProject = createProject(userA, "A second");
    createProject(userB, "B only");

    String response =
        mockMvc
            .perform(get("/api/v1/projects").header("Authorization", bearer(userA)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode projects = objectMapper.readTree(response);

    assertThat(projects).hasSize(2);
    assertThat(projects.findValues("id"))
        .extracting(JsonNode::asLong)
        .containsExactlyInAnyOrder(firstProject, secondProject);
    assertThat(projects.get(0).get("myRole").asText()).isEqualTo("OWNER");
    assertThat(projects.get(0).get("memberCount").asLong()).isEqualTo(1);
  }

  @Test
  void listProjects_includesArchivedProjects() throws Exception {
    TestUser user = registerUser();
    long projectId = createProject(user, "To be archived");
    mockMvc
        .perform(
            put("/api/v1/projects/" + projectId)
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody("To be archived", "desc", "blue", true)))
        .andExpect(status().isOk());

    String response =
        mockMvc
            .perform(get("/api/v1/projects").header("Authorization", bearer(user)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode projects = objectMapper.readTree(response);

    assertThat(projects).hasSize(1);
    assertThat(projects.get(0).get("archived").asBoolean()).isTrue();
  }

  @Test
  void listProjects_admin_seesForeignProjectsWithOwnerRole() throws Exception {
    TestUser user = registerUser();
    TestUser admin = registerAdmin();
    long foreignProject = createProject(user, "Foreign to admin");

    String response =
        mockMvc
            .perform(get("/api/v1/projects").header("Authorization", bearer(admin)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode projects = objectMapper.readTree(response);

    JsonNode foreign = null;
    for (JsonNode project : projects) {
      if (project.get("id").asLong() == foreignProject) {
        foreign = project;
      }
    }
    assertThat(foreign).as("admin listing must include the foreign project").isNotNull();
    assertThat(foreign.get("myRole").asText()).isEqualTo("OWNER");
  }

  @Test
  void listProjects_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/v1/projects"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void createProject_valid_returns201WithOwnerRoleAndIsoTimestamp() throws Exception {
    TestUser user = registerUser();
    String response =
        mockMvc
            .perform(
                post("/api/v1/projects")
                    .header("Authorization", bearer(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Fresh Project","description":null,"colorTag":"purple"}
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Fresh Project"))
            .andExpect(jsonPath("$.description").value((String) null))
            .andExpect(jsonPath("$.colorTag").value("purple"))
            .andExpect(jsonPath("$.archived").value(false))
            .andExpect(jsonPath("$.myRole").value("OWNER"))
            .andExpect(jsonPath("$.memberCount").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String createdAt = objectMapper.readTree(response).get("createdAt").asText();
    assertThatCode(() -> Instant.parse(createdAt))
        .as("createdAt must be an ISO-8601 instant")
        .doesNotThrowAnyException();
  }

  /** Table of invalid create bodies and the field each must flag. */
  static Stream<Arguments> invalidCreateBodies() {
    return Stream.of(
        Arguments.of("{\"name\":\"\",\"colorTag\":\"blue\"}", "name"),
        Arguments.of("{\"name\":\"%s\",\"colorTag\":\"blue\"}".formatted("x".repeat(121)), "name"),
        Arguments.of("{\"name\":\"Valid\",\"colorTag\":\"magenta\"}", "colorTag"),
        Arguments.of("{\"name\":\"Valid\"}", "colorTag"));
  }

  @ParameterizedTest
  @MethodSource("invalidCreateBodies")
  void createProject_invalidBody_returns400WithFieldError(String body, String expectedField)
      throws Exception {
    TestUser user = registerUser();
    mockMvc
        .perform(
            post("/api/v1/projects")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[?(@.field == '%s')]".formatted(expectedField)).exists());
  }

  @Test
  void createProject_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"No auth","colorTag":"red"}
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getProject_asMember_returns200WithMemberRole() throws Exception {
    TestUser owner = registerUser();
    TestUser member = registerUser();
    long projectId = createProject(owner, "Shared project");
    addMember(owner, projectId, member);

    mockMvc
        .perform(get("/api/v1/projects/" + projectId).header("Authorization", bearer(member)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(projectId))
        .andExpect(jsonPath("$.myRole").value("MEMBER"))
        .andExpect(jsonPath("$.memberCount").value(2));
  }

  @Test
  void getProject_asNonMember_returns403() throws Exception {
    TestUser owner = registerUser();
    TestUser outsider = registerUser();
    long projectId = createProject(owner, "Private project");

    mockMvc
        .perform(get("/api/v1/projects/" + projectId).header("Authorization", bearer(outsider)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void getProject_unknownId_returns404() throws Exception {
    TestUser user = registerUser();
    mockMvc
        .perform(get("/api/v1/projects/999999999").header("Authorization", bearer(user)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
  }

  @Test
  void getProject_asAdminNonMember_returns200WithOwnerRole() throws Exception {
    TestUser owner = registerUser();
    TestUser admin = registerAdmin();
    long projectId = createProject(owner, "Admin visible");

    mockMvc
        .perform(get("/api/v1/projects/" + projectId).header("Authorization", bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.myRole").value("OWNER"));
  }

  @Test
  void updateProject_asOwner_returns200WithAllFieldsReplaced() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Before update");

    mockMvc
        .perform(
            put("/api/v1/projects/" + projectId)
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody("After update", "new description", "amber", true)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("After update"))
        .andExpect(jsonPath("$.description").value("new description"))
        .andExpect(jsonPath("$.colorTag").value("amber"))
        .andExpect(jsonPath("$.archived").value(true))
        .andExpect(jsonPath("$.myRole").value("OWNER"));
  }

  @Test
  void updateProject_asPlainMember_returns403() throws Exception {
    TestUser owner = registerUser();
    TestUser member = registerUser();
    long projectId = createProject(owner, "Owner only");
    addMember(owner, projectId, member);

    mockMvc
        .perform(
            put("/api/v1/projects/" + projectId)
                .header("Authorization", bearer(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody("Hijacked", null, "red", false)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void updateProject_asAdminNonMember_returns200() throws Exception {
    TestUser owner = registerUser();
    TestUser admin = registerAdmin();
    long projectId = createProject(owner, "Admin editable");

    mockMvc
        .perform(
            put("/api/v1/projects/" + projectId)
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody("Admin edited", null, "green", false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Admin edited"));
  }

  @Test
  void updateProject_missingArchivedFlag_returns400() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Bad update");

    mockMvc
        .perform(
            put("/api/v1/projects/" + projectId)
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Valid","colorTag":"blue"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[?(@.field == 'archived')]").exists());
  }

  @Test
  void updateProject_unknownId_returns404() throws Exception {
    TestUser user = registerUser();
    mockMvc
        .perform(
            put("/api/v1/projects/999999999")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody("Name", null, "blue", false)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
  }

  private static String updateBody(
      String name, String description, String colorTag, boolean archived) {
    return """
        {"name":"%s","description":%s,"colorTag":"%s","archived":%s}
        """
        .formatted(
            name, description == null ? "null" : "\"" + description + "\"", colorTag, archived);
  }
}
