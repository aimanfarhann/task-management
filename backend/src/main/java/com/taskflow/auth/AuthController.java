package com.taskflow.auth;

import com.taskflow.auth.dto.AuthResponse;
import com.taskflow.auth.dto.LoginRequest;
import com.taskflow.auth.dto.RefreshRequest;
import com.taskflow.auth.dto.RegisterRequest;
import com.taskflow.common.security.AuthenticatedUser;
import com.taskflow.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoints for authentication: register, login, refresh, and logout. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  /**
   * Creates the controller.
   *
   * @param authService the authentication business logic
   */
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * Registers a new account and logs it in.
   *
   * @param request the registration data
   * @return 201 with tokens and the created user
   */
  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
  }

  /**
   * Authenticates with email and password.
   *
   * @param request the credentials
   * @return 200 with tokens and the user
   */
  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  /**
   * Exchanges a refresh token for a rotated token pair.
   *
   * @param request the presented refresh token
   * @return 200 with a fresh token pair
   */
  @PostMapping("/refresh")
  public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return authService.refresh(request);
  }

  /**
   * Revokes the presented refresh token. Requires a valid access token; idempotent.
   *
   * @param currentUser the authenticated caller
   * @param request the refresh token to revoke
   * @return 204 with no body
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CurrentUser AuthenticatedUser currentUser, @Valid @RequestBody RefreshRequest request) {
    authService.logout(currentUser, request);
    return ResponseEntity.noContent().build();
  }
}
