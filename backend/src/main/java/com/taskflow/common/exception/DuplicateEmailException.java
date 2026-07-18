package com.taskflow.common.exception;

/**
 * Thrown when registering with an email address that is already taken (case-insensitively). Maps to
 * HTTP 409 with code {@code DUPLICATE_EMAIL}.
 */
public class DuplicateEmailException extends ConflictException {

  private static final long serialVersionUID = 1L;

  /** Creates the exception with the contract-mandated code and message. */
  public DuplicateEmailException() {
    super("DUPLICATE_EMAIL", "An account with this email already exists");
  }
}
