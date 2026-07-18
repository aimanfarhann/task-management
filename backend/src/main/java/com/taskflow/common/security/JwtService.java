package com.taskflow.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Issues and parses HS256-signed JWT access tokens. The token carries {@code sub} (user id as
 * string) plus {@code role} and {@code email} claims per the API contract.
 */
@Component
public class JwtService {

  private final JwtProperties properties;
  private final SecretKey signingKey;

  /**
   * Creates the service and derives the signing key once from configuration.
   *
   * @param properties JWT settings sourced from the environment
   */
  public JwtService(JwtProperties properties) {
    this.properties = properties;
    this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Issues a signed access token for the given user.
   *
   * @param userId the user's database id, stored as the {@code sub} claim (string)
   * @param email the user's email, stored as the {@code email} claim
   * @param role the user's system role name, stored as the {@code role} claim
   * @return the compact serialized JWT
   */
  public String issueAccessToken(Long userId, String email, String role) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("role", role)
        .claim("email", email)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(properties.accessTokenTtl())))
        // signWith(key) alone would auto-select HS512 for long keys; the contract mandates HS256.
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Verifies a token's signature and expiry and extracts its claims.
   *
   * @param token the compact serialized JWT from the Authorization header
   * @return the claims, or empty if the token is invalid, expired, or malformed
   */
  public Optional<AccessTokenClaims> parseAccessToken(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
      return Optional.of(
          new AccessTokenClaims(
              Long.parseLong(claims.getSubject()),
              claims.get("email", String.class),
              claims.get("role", String.class)));
    } catch (JwtException | IllegalArgumentException e) {
      // Invalid tokens are an expected condition on a public API — not an error state.
      return Optional.empty();
    }
  }

  /**
   * Claims extracted from a verified access token.
   *
   * @param userId the {@code sub} claim parsed back to the user id
   * @param email the {@code email} claim
   * @param role the {@code role} claim
   */
  public record AccessTokenClaims(Long userId, String email, String role) {}
}
