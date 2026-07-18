package com.taskflow.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.common.config.CorsProperties;
import com.taskflow.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless security configuration: JWT bearer authentication, CORS locked to the configured SPA
 * origin, CSRF disabled (no cookies), and JSON error bodies in the contract shape for security
 * rejections.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /** BCrypt cost factor; PRD §6 requires at least 10. */
  private static final int BCRYPT_COST = 10;

  private final JwtAuthFilter jwtAuthFilter;
  private final CorsProperties corsProperties;
  private final ObjectMapper objectMapper;

  /**
   * Creates the configuration.
   *
   * @param jwtAuthFilter authenticates Bearer tokens ahead of the authorization rules
   * @param corsProperties supplies the single allowed SPA origin
   * @param objectMapper writes contract-shaped JSON error bodies for 401/403
   */
  public SecurityConfig(
      JwtAuthFilter jwtAuthFilter, CorsProperties corsProperties, ObjectMapper objectMapper) {
    this.jwtAuthFilter = jwtAuthFilter;
    this.corsProperties = corsProperties;
    this.objectMapper = objectMapper;
  }

  /**
   * Builds the single security filter chain. Register, login, and refresh are public; the OpenAPI
   * spec paths are public but only exist in the dev profile; everything else requires a valid
   * access token.
   *
   * @param http the builder provided by Spring Security
   * @return the configured filter chain
   * @throws Exception if the builder fails
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh")
                    .permitAll()
                    .requestMatchers("/api/docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            handling ->
                handling
                    .authenticationEntryPoint(unauthenticatedEntryPoint())
                    .accessDeniedHandler(accessDeniedHandler()))
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  /**
   * Returns the password encoder used for all password hashing: BCrypt (cost {@value #BCRYPT_COST})
   * wrapped with a SHA-256 pre-hash so contract-valid passwords longer than BCrypt's 72-byte limit
   * are handled without truncation or error.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new Sha256PreHashingPasswordEncoder(new BCryptPasswordEncoder(BCRYPT_COST));
  }

  /**
   * Prevents the servlet container from auto-registering {@link JwtAuthFilter} a second time — it
   * runs only inside the security filter chain.
   *
   * @param filter the JWT filter bean
   * @return a disabled registration
   */
  @Bean
  public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
    FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  /** Returns the CORS configuration locked to the single configured SPA origin (PRD §6). */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(corsProperties.allowedOrigin()));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private AuthenticationEntryPoint unauthenticatedEntryPoint() {
    return (request, response, exception) ->
        writeError(
            response,
            HttpStatus.UNAUTHORIZED,
            ErrorResponse.of("UNAUTHENTICATED", "Authentication required"));
  }

  private AccessDeniedHandler accessDeniedHandler() {
    return (request, response, exception) ->
        writeError(
            response,
            HttpStatus.FORBIDDEN,
            ErrorResponse.of("FORBIDDEN", "You are not permitted to perform this action"));
  }

  private void writeError(HttpServletResponse response, HttpStatus status, ErrorResponse body)
      throws java.io.IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), body);
  }
}
