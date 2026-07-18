package com.taskflow.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * A {@link PasswordEncoder} that SHA-256-hashes the raw password before delegating to an inner
 * encoder (BCrypt).
 *
 * <p>BCrypt only considers the first 72 bytes of its input and, since Spring Security 6.3, throws
 * {@code IllegalArgumentException} for longer input rather than silently truncating. The contract
 * allows passwords up to 100 characters (PRD §5.1, SCHEMA §2.1), so a valid multibyte password can
 * exceed 72 bytes. Pre-hashing to a fixed 44-character Base64 digest keeps every password within
 * BCrypt's limit while preserving the full entropy of the original — no silent truncation and no
 * 500 on a contract-valid input (RULES §22, fail fast). Base64 is used (not the raw digest) so the
 * pre-hash never contains a NUL byte, which BCrypt would itself truncate on.
 */
public class Sha256PreHashingPasswordEncoder implements PasswordEncoder {

  private final PasswordEncoder delegate;

  /**
   * Creates the encoder.
   *
   * @param delegate the inner encoder that hashes the pre-hashed value (e.g. BCrypt)
   */
  public Sha256PreHashingPasswordEncoder(PasswordEncoder delegate) {
    this.delegate = delegate;
  }

  @Override
  public String encode(CharSequence rawPassword) {
    return delegate.encode(sha256Base64(rawPassword));
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    return delegate.matches(sha256Base64(rawPassword), encodedPassword);
  }

  private static String sha256Base64(CharSequence rawPassword) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is mandated by the JDK spec; its absence is an unrecoverable platform defect.
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }
}
