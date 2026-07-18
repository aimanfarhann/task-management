package com.taskflow.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user is not permitted to perform the requested action. Maps to HTTP
 * 403.
 */
public class ForbiddenException extends ApiException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a forbidden exception with the shared {@code FORBIDDEN} code.
   *
   * @param message human-readable explanation, safe to expose to clients
   */
  public ForbiddenException(String message) {
    super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
  }
}
