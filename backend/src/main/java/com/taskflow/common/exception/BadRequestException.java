package com.taskflow.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is well-formed but semantically invalid against the current domain state,
 * e.g. assigning a task to a user who is not a project member. Maps to HTTP 400. Distinct from Bean
 * Validation failures, which the {@link GlobalExceptionHandler} reports with per-field errors.
 */
public class BadRequestException extends ApiException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a bad-request exception.
   *
   * @param code stable machine-readable error code, e.g. {@code ASSIGNEE_NOT_MEMBER}
   * @param message human-readable explanation, safe to expose to clients
   */
  public BadRequestException(String code, String message) {
    super(HttpStatus.BAD_REQUEST, code, message);
  }
}
