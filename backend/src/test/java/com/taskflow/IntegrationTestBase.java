package com.taskflow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for API and repository integration tests. Boots the full application against a single
 * shared Testcontainers PostgreSQL instance (RULES.md §34 — never H2) so Flyway migrations run
 * exactly as in production. Tests isolate their data by registering unique users per test.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

  /** Default password used for every registered test user. */
  protected static final String TEST_PASSWORD = "password123";

  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

  static {
    // Started once for the whole JVM and shared by every test class; Testcontainers' Ryuk
    // sidecar reaps the container when the JVM exits.
    POSTGRES.start();
  }

  @Autowired protected MockMvc mockMvc;
  @Autowired protected ObjectMapper objectMapper;
  @Autowired protected JdbcTemplate jdbcTemplate;

  @DynamicPropertySource
  static void registerContainerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    // Clearly-test-only secret; production reads JWT_SECRET from the environment.
    registry.add(
        "taskflow.jwt.secret", () -> "test-only-jwt-secret-never-use-outside-tests-0123456789");
    registry.add("taskflow.cors.allowed-origin", () -> "http://localhost:4200");
  }

  /**
   * A registered test user with their issued tokens.
   *
   * @param id the user's database id
   * @param email the unique generated email
   * @param accessToken a valid access token
   * @param refreshToken the raw refresh token
   */
  protected record TestUser(long id, String email, String accessToken, String refreshToken) {}

  /** Registers a fresh user with a unique email and returns their ids and tokens. */
  protected TestUser registerUser() throws Exception {
    String email = "user-" + UUID.randomUUID() + "@test.com";
    String response =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerBody(email)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode json = objectMapper.readTree(response);
    return new TestUser(
        json.get("user").get("id").asLong(),
        email,
        json.get("accessToken").asText(),
        json.get("refreshToken").asText());
  }

  /** Registers a fresh user and promotes them to system ADMIN directly in the database. */
  protected TestUser registerAdmin() throws Exception {
    TestUser user = registerUser();
    jdbcTemplate.update("update users set role = 'ADMIN' where id = ?", user.id());
    return user;
  }

  /** Deactivates a user directly in the database, as the M3 admin feature will. */
  protected void deactivateUser(long userId) {
    jdbcTemplate.update("update users set active = false where id = ?", userId);
  }

  /** Creates a project owned by the given user and returns its id. */
  protected long createProject(TestUser owner, String name) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/v1/projects")
                    .header("Authorization", bearer(owner))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"%s","description":"created by tests","colorTag":"blue"}
                        """
                            .formatted(name)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("id").asLong();
  }

  /** Adds a user to a project with the default MEMBER role via the API. */
  protected void addMember(TestUser owner, long projectId, TestUser newMember) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/projects/" + projectId + "/members")
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s"}
                    """
                        .formatted(newMember.email())))
        .andExpect(status().isCreated());
  }

  /** Returns the Authorization header value for the given test user. */
  protected String bearer(TestUser user) {
    return "Bearer " + user.accessToken();
  }

  /** Returns a valid register request body for the given email. */
  protected static String registerBody(String email) {
    return """
        {"email":"%s","password":"%s","displayName":"Test User"}
        """
        .formatted(email, TEST_PASSWORD);
  }
}
