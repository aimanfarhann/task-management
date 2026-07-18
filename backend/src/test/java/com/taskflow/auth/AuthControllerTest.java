package com.taskflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskflow.IntegrationTestBase;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

/** MockMvc integration tests for the auth endpoints. */
class AuthControllerTest extends IntegrationTestBase {

  @Test
  void register_validRequest_returns201WithTokensAndUser() throws Exception {
    String email = "register-" + UUID.randomUUID() + "@test.com";
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.user.id").isNumber())
        .andExpect(jsonPath("$.user.email").value(email))
        .andExpect(jsonPath("$.user.displayName").value("Test User"))
        .andExpect(jsonPath("$.user.role").value("USER"));
  }

  @Test
  void register_duplicateEmailInDifferentCase_returns409DuplicateEmail() throws Exception {
    TestUser existing = registerUser();
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(existing.email().toUpperCase())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
        .andExpect(jsonPath("$.fieldErrors").isEmpty());
  }

  /** Table of invalid registration bodies and the field each must flag. */
  static Stream<Arguments> invalidRegisterBodies() {
    return Stream.of(
        Arguments.of("{\"password\":\"password123\",\"displayName\":\"Name\"}", "email"),
        Arguments.of(
            "{\"email\":\"not-an-email\",\"password\":\"password123\",\"displayName\":\"N\"}",
            "email"),
        Arguments.of(
            "{\"email\":\"short-pw@test.com\",\"password\":\"short\",\"displayName\":\"N\"}",
            "password"),
        Arguments.of(
            "{\"email\":\"blank-name@test.com\",\"password\":\"password123\",\"displayName\":\"\"}",
            "displayName"));
  }

  @ParameterizedTest
  @MethodSource("invalidRegisterBodies")
  void register_invalidBody_returns400WithFieldError(String body, String expectedField)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[?(@.field == '%s')]".formatted(expectedField)).exists());
  }

  @Test
  void login_validCredentials_returns200WithTokens() throws Exception {
    TestUser user = registerUser();
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(user.email(), TEST_PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.user.id").value(user.id()));
  }

  @Test
  void registerAndLogin_with100CharacterPassword_succeed() throws Exception {
    // A contract-valid 100-character password exceeds BCrypt's 72-byte limit; the pre-hashing
    // encoder must handle it end to end rather than returning 500.
    String email = "longpw-" + UUID.randomUUID() + "@test.com";
    String password = "P" + "a".repeat(98) + "9"; // exactly 100 characters
    String body =
        "{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"Long Pw\"}"
            .formatted(email, password);
    mockMvc
        .perform(
            post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").isNotEmpty());

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email, password)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty());
  }

  @Test
  void login_wrongPasswordAndUnknownEmail_return401WithIdenticalMessage() throws Exception {
    TestUser user = registerUser();
    String wrongPasswordMessage =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(user.email(), "wrong-password")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String unknownEmailMessage =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody("ghost-" + UUID.randomUUID() + "@test.com", "any-pass")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    // The contract requires the same message whether the email exists or not.
    assertThat(wrongPasswordMessage).isEqualTo(unknownEmailMessage);
  }

  @Test
  void login_blankPassword_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("someone@test.com", "")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void refresh_validToken_returns200AndRotatesToken() throws Exception {
    TestUser user = registerUser();
    String rotated =
        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refreshBody(user.refreshToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.user.id").value(user.id()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String newRefreshToken = objectMapper.readTree(rotated).get("refreshToken").asText();
    assertThat(newRefreshToken).isNotEqualTo(user.refreshToken());

    // Reusing the rotated (revoked) token must be rejected.
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(user.refreshToken())))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

    // The rotated token remains valid.
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(newRefreshToken)))
        .andExpect(status().isOk());
  }

  @Test
  void refresh_unknownToken_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody("never-issued-token")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
  }

  @Test
  void refresh_blankToken_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody("")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void logout_validToken_returns204AndRevokesRefreshToken() throws Exception {
    TestUser user = registerUser();
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(user.refreshToken())))
        .andExpect(status().isNoContent());

    // The revoked refresh token can no longer be exchanged.
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(user.refreshToken())))
        .andExpect(status().isUnauthorized());

    // Logout is idempotent.
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(user.refreshToken())))
        .andExpect(status().isNoContent());
  }

  @Test
  void logout_withoutAccessToken_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody("some-refresh-token")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void logout_blankRefreshToken_returns400() throws Exception {
    TestUser user = registerUser();
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody("")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void authenticatedRequest_deactivatedUser_returns403AtTheFilter() throws Exception {
    TestUser user = registerUser();
    deactivateUser(user.id());
    mockMvc
        .perform(get("/api/v1/projects").header("Authorization", bearer(user)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));
  }

  @Test
  void authenticatedRequest_garbageToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/v1/projects").header("Authorization", "Bearer garbage"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  private static String loginBody(String email, String password) {
    return """
        {"email":"%s","password":"%s"}
        """
        .formatted(email, password);
  }

  private static String refreshBody(String refreshToken) {
    return """
        {"refreshToken":"%s"}
        """
        .formatted(refreshToken);
  }
}
