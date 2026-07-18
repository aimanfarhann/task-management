package com.taskflow.common.security;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskflow.IntegrationTestBase;
import org.junit.jupiter.api.Test;

/**
 * Verifies CORS preflight advertises the HTTP methods the SPA actually uses. The board's status
 * drag-and-drop issues a cross-origin PATCH, so the allowed methods must include it — otherwise the
 * browser blocks the preflight and the flagship interaction fails in the two-deployable topology.
 */
class CorsConfigTest extends IntegrationTestBase {

  @Test
  void preflight_forPatchStatus_advertisesPatchAsAllowed() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/projects/1/tasks/1/status")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "PATCH"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Methods", containsString("PATCH")));
  }
}
