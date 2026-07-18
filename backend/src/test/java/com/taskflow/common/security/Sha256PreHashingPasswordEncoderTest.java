package com.taskflow.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests for {@link Sha256PreHashingPasswordEncoder}. */
class Sha256PreHashingPasswordEncoderTest {

  private final PasswordEncoder encoder =
      new Sha256PreHashingPasswordEncoder(new BCryptPasswordEncoder(10));

  @Test
  void encodeAndMatches_passwordOver72Bytes_roundTripsWithoutError() {
    // 100 ASCII characters = 100 bytes, past BCrypt's 72-byte limit — the contract allows this.
    String password = "A".repeat(100);

    String hash = encoder.encode(password);

    assertThat(encoder.matches(password, hash)).isTrue();
  }

  @Test
  void encode_veryLongPassword_doesNotThrow() {
    assertThatCode(() -> encoder.encode("x".repeat(200))).doesNotThrowAnyException();
  }

  @Test
  void matches_wrongPassword_returnsFalse() {
    String hash = encoder.encode("A".repeat(100));

    assertThat(encoder.matches("B".repeat(100), hash)).isFalse();
  }

  @Test
  void matches_passwordsDifferingOnlyAfter72Bytes_areDistinguished() {
    // Plain BCrypt ignores bytes past 72 and would treat these as equal; pre-hashing must not.
    String prefix = "z".repeat(72);
    String hash = encoder.encode(prefix + "AAAA");

    assertThat(encoder.matches(prefix + "BBBB", hash)).isFalse();
    assertThat(encoder.matches(prefix + "AAAA", hash)).isTrue();
  }
}
