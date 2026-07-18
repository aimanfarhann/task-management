package com.taskflow.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskflow.IntegrationTestBase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** MockMvc integration tests for the admin user-management endpoints. */
class AdminUserControllerTest extends IntegrationTestBase {

  private static final String USERS_URL = "/api/v1/admin/users";

  @Test
  void listUsers_asAdmin_returns200WithUsersOldestFirst() throws Exception {
    TestUser earlier = registerUser();
    TestUser later = registerUser();
    TestUser admin = registerAdmin();

    String body =
        mockMvc
            .perform(get(USERS_URL).header("Authorization", bearer(admin)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    List<String> emails = emailsInOrder(body);
    // The full user set is shared across tests, so assert on the relative order of ours only.
    assertThat(emails).contains(earlier.email(), later.email(), admin.email());
    assertThat(emails.indexOf(earlier.email())).isLessThan(emails.indexOf(later.email()));
    assertThat(emails.indexOf(later.email())).isLessThan(emails.indexOf(admin.email()));
    assertUserShape(body, admin.email(), "ADMIN", true);
    assertUserShape(body, earlier.email(), "USER", true);
  }

  @Test
  void listUsers_asPlainUser_returns403() throws Exception {
    TestUser user = registerUser();

    mockMvc
        .perform(get(USERS_URL).header("Authorization", bearer(user)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void listUsers_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(get(USERS_URL))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void setActive_asAdminDeactivatesUser_returns200WithInactiveUser() throws Exception {
    TestUser admin = registerAdmin();
    TestUser target = registerUser();

    mockMvc
        .perform(
            patch(activeUrl(target.id()))
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(activeBody(false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(target.id()))
        .andExpect(jsonPath("$.email").value(target.email()))
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.role").value("USER"))
        // createdAt must serialize as an ISO-8601 datetime, not an epoch number, so the SPA parses
        // it.
        .andExpect(jsonPath("$.createdAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*")));
  }

  @Test
  void setActive_adminDeactivatesAnotherAdmin_returns200() throws Exception {
    TestUser actingAdmin = registerAdmin();
    TestUser otherAdmin = registerAdmin();

    // Deactivating another admin is allowed while the acting admin remains active (last-admin
    // guard).
    mockMvc
        .perform(
            patch(activeUrl(otherAdmin.id()))
                .header("Authorization", bearer(actingAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(activeBody(false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.role").value("ADMIN"));
  }

  @Test
  void setActive_unknownUser_returns404UserNotFound() throws Exception {
    TestUser admin = registerAdmin();

    mockMvc
        .perform(
            patch(activeUrl(999999999L))
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(activeBody(false)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
  }

  @Test
  void setActive_adminDeactivatingSelf_returns409CannotDeactivateSelf() throws Exception {
    TestUser admin = registerAdmin();

    mockMvc
        .perform(
            patch(activeUrl(admin.id()))
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(activeBody(false)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CANNOT_DEACTIVATE_SELF"));
  }

  @Test
  void setActive_asPlainUser_returns403() throws Exception {
    TestUser user = registerUser();
    TestUser target = registerUser();

    mockMvc
        .perform(
            patch(activeUrl(target.id()))
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(activeBody(false)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void setActive_missingActiveField_returns400() throws Exception {
    TestUser admin = registerAdmin();
    TestUser target = registerUser();

    mockMvc
        .perform(
            patch(activeUrl(target.id()))
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[?(@.field == 'active')]").exists());
  }

  @Test
  void setActive_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            patch(activeUrl(1L)).contentType(MediaType.APPLICATION_JSON).content(activeBody(false)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void setActive_deactivationRejectsAtFilter_thenReactivationRestoresAccess() throws Exception {
    TestUser admin = registerAdmin();
    TestUser target = registerUser();

    // Baseline: the target can reach an authenticated endpoint.
    mockMvc
        .perform(get("/api/v1/projects").header("Authorization", bearer(target)))
        .andExpect(status().isOk());

    // Deactivate via the admin endpoint — takes effect immediately at the JWT filter.
    mockMvc
        .perform(
            patch(activeUrl(target.id()))
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(activeBody(false)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/v1/projects").header("Authorization", bearer(target)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));

    // Reactivate — the same token works again since the filter re-loads user state per request.
    mockMvc
        .perform(
            patch(activeUrl(target.id()))
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(activeBody(true)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/v1/projects").header("Authorization", bearer(target)))
        .andExpect(status().isOk());
  }

  private List<String> emailsInOrder(String body) throws Exception {
    JsonNode array = objectMapper.readTree(body);
    List<String> emails = new ArrayList<>();
    for (JsonNode node : array) {
      emails.add(node.get("email").asText());
    }
    return emails;
  }

  private void assertUserShape(String body, String email, String role, boolean active)
      throws Exception {
    JsonNode array = objectMapper.readTree(body);
    JsonNode match = null;
    for (JsonNode node : array) {
      if (email.equals(node.get("email").asText())) {
        match = node;
        break;
      }
    }
    assertThat(match).as("user %s present in listing", email).isNotNull();
    assertThat(match.get("role").asText()).isEqualTo(role);
    assertThat(match.get("active").asBoolean()).isEqualTo(active);
    assertThat(match.get("createdAt").asText()).isNotBlank();
    assertThat(match.has("passwordHash")).isFalse();
  }

  private static String activeUrl(long userId) {
    return USERS_URL + "/" + userId + "/active";
  }

  private static String activeBody(boolean active) {
    return """
        {"active":%s}
        """
        .formatted(active);
  }
}
