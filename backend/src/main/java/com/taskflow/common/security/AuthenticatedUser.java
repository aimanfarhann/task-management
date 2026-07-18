package com.taskflow.common.security;

import com.taskflow.user.Role;

/**
 * The authenticated principal placed in the security context by {@link JwtAuthFilter} and injected
 * into controllers via {@link CurrentUser}. Carries only what authorization decisions need — never
 * the JPA entity.
 *
 * @param id the user's database id
 * @param email the user's email address
 * @param role the user's system role
 */
public record AuthenticatedUser(Long id, String email, Role role) {

  /** Returns whether this user holds the system {@code ADMIN} role. */
  public boolean isAdmin() {
    return role == Role.ADMIN;
  }
}
