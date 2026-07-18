package com.taskflow.auth;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodically purges refresh tokens that can no longer be redeemed (expired or revoked). Without
 * this, every register/login/refresh permanently accreted a row — an unbounded, unauthenticated
 * write vector that would slowly degrade the hot {@code token_hash} lookup on refresh.
 */
@Component
public class RefreshTokenCleanup {

  private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanup.class);

  private final RefreshTokenRepository refreshTokenRepository;

  /**
   * Creates the cleanup job.
   *
   * @param refreshTokenRepository data access used to delete dead tokens
   */
  public RefreshTokenCleanup(RefreshTokenRepository refreshTokenRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
  }

  /** Deletes expired and revoked refresh tokens once daily at 03:00 server time. */
  @Scheduled(cron = "0 0 3 * * *")
  @Transactional
  public void purgeDeadTokens() {
    int removed = refreshTokenRepository.deleteDeadTokens(Instant.now());
    if (removed > 0) {
      log.info("Purged {} expired or revoked refresh tokens", removed);
    }
  }
}
