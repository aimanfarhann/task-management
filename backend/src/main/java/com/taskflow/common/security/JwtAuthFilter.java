package com.taskflow.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.common.exception.ErrorResponse;
import com.taskflow.user.User;
import com.taskflow.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests carrying a {@code Bearer} access token. The user is re-loaded from the
 * database on every request so that role changes and deactivation take effect immediately —
 * inactive users are rejected here with 403 (SCHEMA.md §4).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final UserService userService;
  private final ObjectMapper objectMapper;

  /**
   * Creates the filter.
   *
   * @param jwtService verifies and parses access tokens
   * @param userService loads the current user state for freshness checks
   * @param objectMapper writes the contract error body on rejection
   */
  public JwtAuthFilter(JwtService jwtService, UserService userService, ObjectMapper objectMapper) {
    this.jwtService = jwtService;
    this.userService = userService;
    this.objectMapper = objectMapper;
  }

  /**
   * Populates the security context from a valid Bearer token; requests without a valid token
   * continue unauthenticated and are rejected downstream by the authorization rules.
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith(BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    Optional<JwtService.AccessTokenClaims> claims =
        jwtService.parseAccessToken(header.substring(BEARER_PREFIX.length()));
    if (claims.isEmpty()) {
      filterChain.doFilter(request, response);
      return;
    }

    Optional<User> user = userService.findById(claims.get().userId());
    if (user.isEmpty()) {
      filterChain.doFilter(request, response);
      return;
    }
    if (!user.get().isActive()) {
      writeInactiveRejection(response);
      return;
    }

    AuthenticatedUser principal =
        new AuthenticatedUser(user.get().getId(), user.get().getEmail(), user.get().getRole());
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    filterChain.doFilter(request, response);
  }

  private void writeInactiveRejection(HttpServletResponse response) throws IOException {
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getWriter(),
        ErrorResponse.of("ACCOUNT_INACTIVE", "This account has been deactivated"));
  }
}
