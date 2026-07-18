package com.taskflow.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request conflicts with the current state of a resource, e.g. adding a user who is
 * already a member. Maps to HTTP 409.
 */
public class ConflictException extends ApiException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a conflict exception.
   *
   * @param code error code, e.g. {@code ALREADY_MEMBER} or {@code LAST_OWNER}
   * @param message human-readable explanation, safe to expose to clients
   */
  public ConflictException(String code, String message) {
    super(HttpStatus.CONFLICT, code, message);
  }
}
