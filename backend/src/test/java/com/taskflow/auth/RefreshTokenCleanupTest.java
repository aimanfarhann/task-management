package com.taskflow.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link RefreshTokenCleanup}. */
@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks private RefreshTokenCleanup refreshTokenCleanup;

  @Test
  void purgeDeadTokens_delegatesDeletionToTheRepository() {
    // Arrange
    when(refreshTokenRepository.deleteDeadTokens(any(Instant.class))).thenReturn(3);

    // Act
    refreshTokenCleanup.purgeDeadTokens();

    // Assert
    verify(refreshTokenRepository).deleteDeadTokens(any(Instant.class));
  }
}
