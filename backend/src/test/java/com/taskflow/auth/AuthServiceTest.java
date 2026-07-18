package com.taskflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskflow.auth.dto.AuthResponse;
import com.taskflow.auth.dto.LoginRequest;
import com.taskflow.auth.dto.RefreshRequest;
import com.taskflow.auth.dto.RegisterRequest;
import com.taskflow.common.exception.DuplicateEmailException;
import com.taskflow.common.exception.UnauthorizedException;
import com.taskflow.common.security.JwtProperties;
import com.taskflow.common.security.JwtService;
import com.taskflow.testutil.TestData;
import com.taskflow.user.User;
import com.taskflow.user.UserService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests for {@link AuthService} business rules. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final String RAW_REFRESH_TOKEN = "raw-refresh-token-value";

  @Mock private UserService userService;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    JwtProperties jwtProperties =
        new JwtProperties(
            "unit-test-secret-unit-test-secret-unit", Duration.ofMinutes(60), Duration.ofDays(7));
    authService =
        new AuthService(
            userService, refreshTokenRepository, passwordEncoder, jwtService, jwtProperties);
  }

  @Test
  void register_newEmail_createsUserAndIssuesTokenPair() {
    // Arrange
    RegisterRequest request = new RegisterRequest("new@test.com", "password123", "New User");
    User user = TestData.user(1L, "new@test.com");
    when(passwordEncoder.encode("password123")).thenReturn("encoded-hash");
    when(userService.createUser("new@test.com", "encoded-hash", "New User")).thenReturn(user);
    when(jwtService.issueAccessToken(1L, "new@test.com", "USER")).thenReturn("access-token");
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    AuthResponse response = authService.register(request);

    // Assert
    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.refreshToken()).isNotBlank();
    assertThat(response.user().id()).isEqualTo(1L);
    assertThat(response.user().email()).isEqualTo("new@test.com");
    ArgumentCaptor<RefreshToken> savedToken = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(savedToken.capture());
    // Only the SHA-256 hex digest is persisted, never the raw value.
    assertThat(savedToken.getValue().getTokenHash())
        .hasSize(64)
        .isNotEqualTo(response.refreshToken());
    assertThat(savedToken.getValue().getExpiresAt()).isAfter(Instant.now());
  }

  @Test
  void register_duplicateEmail_propagatesConflictWithoutIssuingTokens() {
    // Arrange
    RegisterRequest request = new RegisterRequest("taken@test.com", "password123", "New User");
    when(passwordEncoder.encode("password123")).thenReturn("encoded-hash");
    when(userService.createUser("taken@test.com", "encoded-hash", "New User"))
        .thenThrow(new DuplicateEmailException());

    // Act + Assert
    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(DuplicateEmailException.class);
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  void login_validCredentials_returnsTokenPair() {
    // Arrange
    User user = TestData.user(1L, "user@test.com");
    when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
    when(jwtService.issueAccessToken(1L, "user@test.com", "USER")).thenReturn("access-token");
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    AuthResponse response = authService.login(new LoginRequest("user@test.com", "password123"));

    // Assert
    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.user().id()).isEqualTo(1L);
  }

  @Test
  void login_unknownEmail_throwsInvalidCredentials() {
    // Arrange
    when(userService.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

    // Act + Assert
    assertInvalidCredentials(() -> authService.login(new LoginRequest("ghost@test.com", "pw")));
  }

  @Test
  void login_wrongPassword_throwsInvalidCredentials() {
    // Arrange
    User user = TestData.user(1L, "user@test.com");
    when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);

    // Act + Assert
    assertInvalidCredentials(() -> authService.login(new LoginRequest("user@test.com", "wrong")));
  }

  @Test
  void login_inactiveUser_throwsInvalidCredentials() {
    // Arrange
    User user = TestData.inactiveUser(1L, "user@test.com");
    when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));

    // Act + Assert
    assertInvalidCredentials(
        () -> authService.login(new LoginRequest("user@test.com", "password123")));
  }

  @Test
  void refresh_validToken_rotatesOldTokenAndIssuesNewPair() {
    // Arrange
    User user = TestData.user(1L, "user@test.com");
    RefreshToken stored = new RefreshToken(user, "stored-hash", Instant.now().plusSeconds(3600));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
    when(refreshTokenRepository.revokeIfActive(anyString(), any(Instant.class))).thenReturn(1);
    when(jwtService.issueAccessToken(1L, "user@test.com", "USER")).thenReturn("new-access-token");
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    AuthResponse response = authService.refresh(new RefreshRequest(RAW_REFRESH_TOKEN));

    // Assert: the presented token is atomically revoked (rotation) and a fresh one is persisted.
    verify(refreshTokenRepository).revokeIfActive(anyString(), any(Instant.class));
    assertThat(response.accessToken()).isEqualTo("new-access-token");
    assertThat(response.refreshToken()).isNotEqualTo(RAW_REFRESH_TOKEN);
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  void refresh_unknownToken_throwsInvalidRefreshToken() {
    // Arrange
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    // Act + Assert
    assertInvalidRefreshToken(() -> authService.refresh(new RefreshRequest("unknown")));
  }

  @Test
  void refresh_revokedToken_rejectsReuse() {
    // Arrange: the token was already rotated once, so the atomic revoke changes no rows.
    User user = TestData.user(1L, "user@test.com");
    RefreshToken stored = new RefreshToken(user, "stored-hash", Instant.now().plusSeconds(3600));
    stored.revoke(Instant.now());
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
    when(refreshTokenRepository.revokeIfActive(anyString(), any(Instant.class))).thenReturn(0);

    // Act + Assert
    assertInvalidRefreshToken(() -> authService.refresh(new RefreshRequest(RAW_REFRESH_TOKEN)));
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  void refresh_concurrentRedemption_rejectsTheLoser() {
    // Arrange: the token is valid on read, but a concurrent request rotates it first, so the
    // atomic revoke changes zero rows for this caller.
    User user = TestData.user(1L, "user@test.com");
    RefreshToken stored = new RefreshToken(user, "stored-hash", Instant.now().plusSeconds(3600));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
    when(refreshTokenRepository.revokeIfActive(anyString(), any(Instant.class))).thenReturn(0);

    // Act + Assert
    assertInvalidRefreshToken(() -> authService.refresh(new RefreshRequest(RAW_REFRESH_TOKEN)));
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  void refresh_expiredToken_throwsInvalidRefreshToken() {
    // Arrange
    User user = TestData.user(1L, "user@test.com");
    RefreshToken stored = new RefreshToken(user, "stored-hash", Instant.now().minusSeconds(60));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

    // Act + Assert
    assertInvalidRefreshToken(() -> authService.refresh(new RefreshRequest(RAW_REFRESH_TOKEN)));
  }

  @Test
  void refresh_inactiveUser_throwsInvalidRefreshToken() {
    // Arrange
    User user = TestData.inactiveUser(1L, "user@test.com");
    RefreshToken stored = new RefreshToken(user, "stored-hash", Instant.now().plusSeconds(3600));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

    // Act + Assert
    assertInvalidRefreshToken(() -> authService.refresh(new RefreshRequest(RAW_REFRESH_TOKEN)));
  }

  @Test
  void logout_ownActiveToken_revokesIt() {
    // Arrange
    User user = TestData.user(1L, "user@test.com");
    RefreshToken stored = new RefreshToken(user, "stored-hash", Instant.now().plusSeconds(3600));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

    // Act
    authService.logout(TestData.authUser(1L), new RefreshRequest(RAW_REFRESH_TOKEN));

    // Assert
    assertThat(stored.isRevoked()).isTrue();
  }

  @Test
  void logout_unknownToken_isIdempotent() {
    // Arrange
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    // Act + Assert
    assertThatCode(
            () -> authService.logout(TestData.authUser(1L), new RefreshRequest("unknown-token")))
        .doesNotThrowAnyException();
  }

  @Test
  void logout_anotherUsersToken_leavesItUntouched() {
    // Arrange
    User otherUser = TestData.user(2L, "other@test.com");
    RefreshToken stored =
        new RefreshToken(otherUser, "stored-hash", Instant.now().plusSeconds(3600));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

    // Act
    authService.logout(TestData.authUser(1L), new RefreshRequest(RAW_REFRESH_TOKEN));

    // Assert
    assertThat(stored.isRevoked()).isFalse();
  }

  private static void assertInvalidCredentials(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
    assertThatThrownBy(action)
        .isInstanceOfSatisfying(
            UnauthorizedException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("INVALID_CREDENTIALS"));
  }

  private static void assertInvalidRefreshToken(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
    assertThatThrownBy(action)
        .isInstanceOfSatisfying(
            UnauthorizedException.class,
            exception -> assertThat(exception.getCode()).isEqualTo("INVALID_REFRESH_TOKEN"));
  }
}
