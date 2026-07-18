package com.taskflow.auth;

import com.taskflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mirroring the {@code refresh_tokens} table (SCHEMA.md §2.6). Stores only the SHA-256
 * hash of the opaque token — the raw value never touches the database (RULES.md §29).
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token_hash", nullable = false, length = 64, unique = true)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  /** JPA-only constructor. */
  protected RefreshToken() {}

  /**
   * Creates an unrevoked refresh token record.
   *
   * @param user the owning user
   * @param tokenHash SHA-256 hex digest of the raw token value
   * @param expiresAt the absolute expiry timestamp
   */
  public RefreshToken(User user, String tokenHash, Instant expiresAt) {
    this.id = UUID.randomUUID();
    this.user = user;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  /** Returns the token record id. */
  public UUID getId() {
    return id;
  }

  /** Returns the owning user. */
  public User getUser() {
    return user;
  }

  /** Returns the SHA-256 hex digest of the raw token value. */
  public String getTokenHash() {
    return tokenHash;
  }

  /** Returns the absolute expiry timestamp. */
  public Instant getExpiresAt() {
    return expiresAt;
  }

  /** Returns the revocation timestamp, or null if the token is still valid. */
  public Instant getRevokedAt() {
    return revokedAt;
  }

  /** Returns whether this token has been revoked (rotation or logout). */
  public boolean isRevoked() {
    return revokedAt != null;
  }

  /**
   * Returns whether this token is expired at the given instant.
   *
   * @param now the reference instant
   * @return true if the expiry lies before {@code now}
   */
  public boolean isExpired(Instant now) {
    return expiresAt.isBefore(now);
  }

  /**
   * Marks this token as revoked. Idempotent — an already revoked token keeps its original
   * revocation timestamp.
   *
   * @param now the revocation instant
   */
  public void revoke(Instant now) {
    if (revokedAt == null) {
      this.revokedAt = now;
    }
  }
}
