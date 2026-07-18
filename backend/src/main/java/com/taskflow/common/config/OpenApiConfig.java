package com.taskflow.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * OpenAPI metadata for the generated spec at {@code /api/docs}. Dev profile only — the spec is
 * disabled outside dev (ARCHITECTURE.md §3.5).
 */
@Configuration
@Profile("dev")
public class OpenApiConfig {

  /** Returns the API metadata shown in the generated OpenAPI document. */
  @Bean
  public OpenAPI taskflowOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("TaskFlow API")
                .description("REST API for the TaskFlow task and project management application")
                .version("v1"));
  }
}
