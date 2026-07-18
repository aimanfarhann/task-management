package com.taskflow.auth;

import com.taskflow.auth.dto.AuthResponse;
import com.taskflow.auth.dto.LoginRequest;
import com.taskflow.auth.dto.RefreshRequest;
import com.taskflow.auth.dto.RegisterRequest;
import com.taskflow.common.exception.UnauthorizedException;
import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.common.security.JwtProperties;
import com.taskflow.common.security.JwtService;
import com.taskflow.user.User;
import com.taskflow.user.UserService;
import com.taskflow.user.dto.UserMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication business logic: registration, login, refresh token rotation, and logout. Access
 * tokens are stateless JWTs; refresh tokens are opaque 256-bit random values stored SHA-256-hashed
 * and rotated on every refresh (ARCHITECTURE.md §5). Tokens and passwords are never logged.
 */
@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int REFRESH_TOKEN_BYTES = 32;

  private final UserService userService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;

  /**
   * A precomputed hash compared against when no matching user exists, so every login runs exactly
   * one password comparison and its latency cannot reveal whether an account exists (see {@link
   * #login}). Computed with the configured encoder so its cost matches real hashes.
   */
  private final String timingDecoyHash;

  /**
   * Creates the service.
   *
   * @param userService user domain operations (creation, lookup)
   * @param refreshTokenRepository data access for stored refresh token hashes
   * @param passwordEncoder BCrypt encoder for password hashing and verification
   * @param jwtService issues signed access tokens
   * @param jwtProperties supplies the refresh token lifetime
   */
  public AuthService(
      UserService userService,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      JwtProperties jwtProperties) {
    this.userService = userService;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.jwtProperties = jwtProperties;
    this.timingDecoyHash = passwordEncoder.encode("uniform-login-timing-decoy");
  }

  /**
   * Registers a new user and logs them in immediately.
   *
   * @param request the validated registration data
   * @return tokens plus the created user
   * @throws com.taskflow.common.exception.DuplicateEmailException if the email is already taken
   */
  @Transactional
  public AuthResponse register(RegisterRequest request) {
    User user =
        userService.createUser(
            request.email(), passwordEncoder.encode(request.password()), request.displayName());
    log.info("User {} registered", user.getId());
    return issueTokens(user);
  }

  /**
   * Authenticates a user by email and password. Unknown email, wrong password, and inactive account
   * all produce the same 401 so that account existence is not disclosed.
   *
   * @param request the login credentials
   * @return tokens plus the authenticated user
   * @throws UnauthorizedException with code {@code INVALID_CREDENTIALS} on any failure
   */
  @Transactional
  public AuthResponse login(LoginRequest request) {
    User user = userService.findByEmail(request.email()).filter(User::isActive).orElse(null);
    // Always run exactly one password comparison — against a decoy hash when the account is unknown
    // or inactive — so response latency cannot reveal which accounts exist (uniform timing).
    String passwordHash = user != null ? user.getPasswordHash() : timingDecoyHash;
    boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);
    if (user == null || !passwordMatches) {
      throw invalidCredentials();
    }
    log.info("User {} logged in", user.getId());
    return issueTokens(user);
  }

  /**
   * Exchanges a valid refresh token for a new token pair, revoking the presented token (rotation).
   * A revoked, expired, or unknown token — including reuse of a rotated token — is rejected.
   *
   * @param request the presented refresh token
   * @return a fresh token pair for the token's owner
   * @throws UnauthorizedException with code {@code INVALID_REFRESH_TOKEN} on any failure
   */
  @Transactional
  public AuthResponse refresh(RefreshRequest request) {
    Instant now = Instant.now();
    String tokenHash = sha256Hex(request.refreshToken());
    RefreshToken stored =
        refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(AuthService::invalidRefreshToken);
    if (stored.isExpired(now)) {
      throw invalidRefreshToken();
    }
    User user = stored.getUser();
    if (!user.isActive()) {
      throw invalidRefreshToken();
    }
    // Atomic rotation: only the transaction that flips revoked_at from null wins. A refresh token
    // presented twice concurrently (or reused after rotation) revokes zero rows here and is
    // rejected, closing the check-then-act race that could otherwise mint two valid token pairs.
    if (refreshTokenRepository.revokeIfActive(tokenHash, now) == 0) {
      throw invalidRefreshToken();
    }
    log.info("Rotated refresh token for user {}", user.getId());
    return issueTokens(user);
  }

  /**
   * Revokes the presented refresh token if it belongs to the caller. Idempotent: unknown, foreign,
   * or already revoked tokens are ignored so repeated logouts always succeed.
   *
   * @param currentUser the authenticated caller
   * @param request the refresh token to revoke
   */
  @Transactional
  public void logout(AuthenticatedUser currentUser, RefreshRequest request) {
    refreshTokenRepository
        .findByTokenHash(sha256Hex(request.refreshToken()))
        .filter(stored -> stored.getUser().getId().equals(currentUser.id()))
        .ifPresent(stored -> stored.revoke(Instant.now()));
    log.info("User {} logged out", currentUser.id());
  }

  private AuthResponse issueTokens(User user) {
    String accessToken =
        jwtService.issueAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    String refreshTokenValue = generateRefreshTokenValue();
    refreshTokenRepository.save(
        new RefreshToken(
            user,
            sha256Hex(refreshTokenValue),
            Instant.now().plus(jwtProperties.refreshTokenTtl())));
    return new AuthResponse(accessToken, refreshTokenValue, UserMapper.toDto(user));
  }

  private static String generateRefreshTokenValue() {
    byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is mandated by the JDK spec; its absence is an unrecoverable platform defect.
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  private static UnauthorizedException invalidCredentials() {
    return new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password");
  }

  private static UnauthorizedException invalidRefreshToken() {
    return new UnauthorizedException(
        "INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired");
  }
}
