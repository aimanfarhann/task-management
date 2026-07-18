package com.taskflow.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link JwtService}. */
class JwtServiceTest {

  private static final String SECRET = "unit-test-secret-unit-test-secret-unit-test-secret";
  private static final String OTHER_SECRET = "another-secret-another-secret-another-secret!!";

  private final JwtService jwtService =
      new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(60), Duration.ofDays(7)));

  @Test
  void issueAccessToken_thenParse_roundTripsAllClaims() {
    // Arrange + Act
    String token = jwtService.issueAccessToken(42L, "user@test.com", "ADMIN");
    Optional<JwtService.AccessTokenClaims> claims = jwtService.parseAccessToken(token);

    // Assert
    assertThat(claims).isPresent();
    assertThat(claims.get().userId()).isEqualTo(42L);
    assertThat(claims.get().email()).isEqualTo("user@test.com");
    assertThat(claims.get().role()).isEqualTo("ADMIN");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "garbage", "a.b.c", "eyJhbGciOiJIUzI1NiJ9.e30."})
  void parseAccessToken_malformedToken_returnsEmpty(String token) {
    // Act + Assert
    assertThat(jwtService.parseAccessToken(token)).isEmpty();
  }

  @Test
  void parseAccessToken_tokenSignedWithDifferentSecret_returnsEmpty() {
    // Arrange
    JwtService otherService =
        new JwtService(new JwtProperties(OTHER_SECRET, Duration.ofMinutes(60), Duration.ofDays(7)));
    String foreignToken = otherService.issueAccessToken(42L, "user@test.com", "USER");

    // Act + Assert
    assertThat(jwtService.parseAccessToken(foreignToken)).isEmpty();
  }

  @Test
  void parseAccessToken_expiredToken_returnsEmpty() {
    // Arrange: a service whose tokens are already expired at issue time.
    JwtService expiredIssuer =
        new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(-5), Duration.ofDays(7)));
    String expiredToken = expiredIssuer.issueAccessToken(42L, "user@test.com", "USER");

    // Act + Assert
    assertThat(jwtService.parseAccessToken(expiredToken)).isEmpty();
  }
}
