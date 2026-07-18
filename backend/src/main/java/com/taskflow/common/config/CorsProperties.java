package com.taskflow.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * CORS settings bound from {@code taskflow.cors.*}. The API accepts browser requests from exactly
 * one origin — the SPA (PRD §6 security).
 *
 * @param allowedOrigin the single origin allowed to call the API from a browser
 */
@ConfigurationProperties(prefix = "taskflow.cors")
public record CorsProperties(String allowedOrigin) {

  /** Fails fast at startup when no origin is configured. */
  public CorsProperties {
    Assert.hasText(
        allowedOrigin,
        "taskflow.cors.allowed-origin must be set (CORS_ALLOWED_ORIGIN environment variable)");
  }
}
