package com.taskflow.project;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskflow.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** MockMvc integration tests for the project member endpoints. */
class ProjectMemberControllerTest extends IntegrationTestBase {

  @Test
  void listMembers_asMember_returns200WithAllMembers() throws Exception {
    TestUser owner = registerUser();
    TestUser member = registerUser();
    long projectId = createProject(owner, "Membered");
    addMember(owner, projectId, member);

    mockMvc
        .perform(get(membersUrl(projectId)).header("Authorization", bearer(member)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].userId").value(owner.id()))
        .andExpect(jsonPath("$[0].email").value(owner.email()))
        .andExpect(jsonPath("$[0].projectRole").value("OWNER"))
        .andExpect(jsonPath("$[0].joinedAt").isNotEmpty())
        .andExpect(jsonPath("$[1].userId").value(member.id()))
        .andExpect(jsonPath("$[1].projectRole").value("MEMBER"));
  }

  @Test
  void listMembers_asNonMember_returns403() throws Exception {
    TestUser owner = registerUser();
    TestUser outsider = registerUser();
    long projectId = createProject(owner, "Closed");

    mockMvc
        .perform(get(membersUrl(projectId)).header("Authorization", bearer(outsider)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void listMembers_unknownProject_returns404() throws Exception {
    TestUser user = registerUser();
    mockMvc
        .perform(get(membersUrl(999999999L)).header("Authorization", bearer(user)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
  }

  @Test
  void listMembers_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(get(membersUrl(1L)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void addMember_asOwnerWithoutRole_returns201DefaultingToMember() throws Exception {
    TestUser owner = registerUser();
    TestUser invited = registerUser();
    long projectId = createProject(owner, "Growing");

    mockMvc
        .perform(
            post(membersUrl(projectId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberBody(invited.email(), null)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(invited.id()))
        .andExpect(jsonPath("$.email").value(invited.email()))
        .andExpect(jsonPath("$.displayName").value("Test User"))
        .andExpect(jsonPath("$.projectRole").value("MEMBER"))
        .andExpect(jsonPath("$.joinedAt").isNotEmpty());
  }

  @Test
  void addMember_asOwnerWithOwnerRole_returns201AsOwner() throws Exception {
    TestUser owner = registerUser();
    TestUser coOwner = registerUser();
    long projectId = createProject(owner, "Co-owned");

    mockMvc
        .perform(
            post(membersUrl(projectId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberBody(coOwner.email(), "OWNER")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectRole").value("OWNER"));
  }

  @Test
  void addMember_unknownEmail_returns404UserNotFound() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Lonely");

    mockMvc
        .perform(
            post(membersUrl(projectId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberBody("ghost-nobody@test.com", null)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
  }

  @Test
  void addMember_alreadyMember_returns409AlreadyMember() throws Exception {
    TestUser owner = registerUser();
    TestUser member = registerUser();
    long projectId = createProject(owner, "Doubled");
    addMember(owner, projectId, member);

    mockMvc
        .perform(
            post(membersUrl(projectId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberBody(member.email(), null)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ALREADY_MEMBER"));
  }

  @Test
  void addMember_asPlainMember_returns403() throws Exception {
    TestUser owner = registerUser();
    TestUser member = registerUser();
    TestUser invited = registerUser();
    long projectId = createProject(owner, "Owner managed");
    addMember(owner, projectId, member);

    mockMvc
        .perform(
            post(membersUrl(projectId))
                .header("Authorization", bearer(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberBody(invited.email(), null)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void addMember_invalidEmail_returns400() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Validated");

    mockMvc
        .perform(
            post(membersUrl(projectId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberBody("not-an-email", null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());
  }

  @Test
  void addMember_asAdminNonMember_returns201() throws Exception {
    TestUser owner = registerUser();
    TestUser admin = registerAdmin();
    TestUser invited = registerUser();
    long projectId = createProject(owner, "Admin managed");

    mockMvc
        .perform(
            post(membersUrl(projectId))
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberBody(invited.email(), null)))
        .andExpect(status().isCreated());
  }

  @Test
  void removeMember_ownerRemovesMember_returns204() throws Exception {
    TestUser owner = registerUser();
    TestUser member = registerUser();
    long projectId = createProject(owner, "Shrinking");
    addMember(owner, projectId, member);

    mockMvc
        .perform(
            delete(membersUrl(projectId) + "/" + member.id())
                .header("Authorization", bearer(owner)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get(membersUrl(projectId)).header("Authorization", bearer(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void removeMember_memberLeavesThemselves_returns204() throws Exception {
    TestUser owner = registerUser();
    TestUser member = registerUser();
    long projectId = createProject(owner, "Leavable");
    addMember(owner, projectId, member);

    mockMvc
        .perform(
            delete(membersUrl(projectId) + "/" + member.id())
                .header("Authorization", bearer(member)))
        .andExpect(status().isNoContent());
  }

  @Test
  void removeMember_memberRemovingSomeoneElse_returns403() throws Exception {
    TestUser owner = registerUser();
    TestUser member = registerUser();
    long projectId = createProject(owner, "Protected");
    addMember(owner, projectId, member);

    mockMvc
        .perform(
            delete(membersUrl(projectId) + "/" + owner.id())
                .header("Authorization", bearer(member)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void removeMember_lastOwner_returns409LastOwner() throws Exception {
    TestUser owner = registerUser();
    long projectId = createProject(owner, "Single owner");

    mockMvc
        .perform(
            delete(membersUrl(projectId) + "/" + owner.id()).header("Authorization", bearer(owner)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("LAST_OWNER"));
  }

  @Test
  void removeMember_ownerWithCoOwner_returns204() throws Exception {
    TestUser owner = registerUser();
    TestUser coOwner = registerUser();
    long projectId = createProject(owner, "Two owners");
    mockMvc
        .perform(
            post(membersUrl(projectId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(memberBody(coOwner.email(), "OWNER")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            delete(membersUrl(projectId) + "/" + owner.id()).header("Authorization", bearer(owner)))
        .andExpect(status().isNoContent());
  }

  @Test
  void removeMember_targetNotAMember_returns404() throws Exception {
    TestUser owner = registerUser();
    TestUser outsider = registerUser();
    long projectId = createProject(owner, "No such member");

    mockMvc
        .perform(
            delete(membersUrl(projectId) + "/" + outsider.id())
                .header("Authorization", bearer(owner)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
  }

  @Test
  void removeMember_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(delete(membersUrl(1L) + "/1"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  private static String membersUrl(long projectId) {
    return "/api/v1/projects/" + projectId + "/members";
  }

  private static String memberBody(String email, String projectRole) {
    if (projectRole == null) {
      return """
          {"email":"%s"}
          """
          .formatted(email);
    }
    return """
        {"email":"%s","projectRole":"%s"}
        """
        .formatted(email, projectRole);
  }
}
