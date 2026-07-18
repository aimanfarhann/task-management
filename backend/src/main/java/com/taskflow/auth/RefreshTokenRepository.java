package com.taskflow.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link RefreshToken}. Called only by the auth feature (RULES.md §21). */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  /**
   * Finds a token record by the SHA-256 hex digest of the presented raw token.
   *
   * @param tokenHash the SHA-256 hex digest
   * @return the token record, or empty if no such token was ever issued
   */
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /**
   * Atomically revokes a token only if it is currently active (not yet revoked), returning the
   * number of rows changed. Used to make refresh-token rotation race-safe: concurrent redemptions
   * of the same token cause all but one to change zero rows.
   *
   * @param tokenHash the SHA-256 hex digest of the token to revoke
   * @param now the revocation timestamp
   * @return 1 if the token was active and is now revoked, 0 if it was already revoked or unknown
   */
  @Modifying
  @Query(
      "update RefreshToken rt set rt.revokedAt = :now"
          + " where rt.tokenHash = :tokenHash and rt.revokedAt is null")
  int revokeIfActive(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

  /**
   * Deletes tokens that can never be redeemed again — expired or revoked — to bound table growth.
   *
   * @param now the reference instant; tokens expiring before it are removed
   * @return the number of rows deleted
   */
  @Modifying
  @Query("delete from RefreshToken rt where rt.expiresAt < :now or rt.revokedAt is not null")
  int deleteDeadTokens(@Param("now") Instant now);
}
