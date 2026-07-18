package com.taskflow.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a requested resource does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends ApiException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a not-found exception.
   *
   * @param code resource-specific error code, e.g. {@code PROJECT_NOT_FOUND}
   * @param message human-readable explanation, safe to expose to clients
   */
  public ResourceNotFoundException(String code, String message) {
    super(HttpStatus.NOT_FOUND, code, message);
  }
}
