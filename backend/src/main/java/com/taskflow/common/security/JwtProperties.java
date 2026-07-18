package com.taskflow.common.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * JWT settings bound from {@code taskflow.jwt.*}. The secret has no default and must be supplied
 * via the {@code JWT_SECRET} environment variable (RULES.md §28).
 *
 * @param secret HS256 signing secret, at least 32 bytes
 * @param accessTokenTtl lifetime of access tokens (60 minutes per PRD)
 * @param refreshTokenTtl lifetime of refresh tokens (7 days per PRD)
 */
@ConfigurationProperties(prefix = "taskflow.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {

  /** Fails fast at startup on a missing or dangerously short secret. */
  public JwtProperties {
    Assert.hasText(secret, "taskflow.jwt.secret must be set (JWT_SECRET environment variable)");
    Assert.isTrue(secret.length() >= 32, "taskflow.jwt.secret must be at least 32 characters");
    Assert.notNull(accessTokenTtl, "taskflow.jwt.access-token-ttl must be set");
    Assert.notNull(refreshTokenTtl, "taskflow.jwt.refresh-token-ttl must be set");
  }
}
