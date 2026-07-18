package com.taskflow.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** JPA entity mirroring the {@code users} table (SCHEMA.md §2.1). */
@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** JPA-only constructor. */
  protected User() {}

  /**
   * Creates a new active user with the default {@code USER} role.
   *
   * @param email the user's email address
   * @param passwordHash the BCrypt hash of the user's password — never the raw password
   * @param displayName the user's display name
   */
  public User(String email, String passwordHash, String displayName) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.role = Role.USER;
    this.active = true;
    this.createdAt = Instant.now();
  }

  /** Returns the database id, or null if not yet persisted. */
  public Long getId() {
    return id;
  }

  /** Returns the email address. */
  public String getEmail() {
    return email;
  }

  /** Returns the BCrypt password hash. */
  public String getPasswordHash() {
    return passwordHash;
  }

  /** Returns the display name. */
  public String getDisplayName() {
    return displayName;
  }

  /** Returns the system role. */
  public Role getRole() {
    return role;
  }

  /** Returns whether the account is active; inactive accounts are rejected at the JWT filter. */
  public boolean isActive() {
    return active;
  }

  /** Returns the creation timestamp. */
  public Instant getCreatedAt() {
    return createdAt;
  }
}
