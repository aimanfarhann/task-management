package com.taskflow.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class of the domain exception hierarchy (ARCHITECTURE.md §3.4). Every subclass carries the
 * HTTP status and the stable machine-readable error code that {@link GlobalExceptionHandler}
 * translates into the API error contract.
 */
public abstract class ApiException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final HttpStatus status;
  private final String code;

  /**
   * Creates a domain exception.
   *
   * @param status the HTTP status the API responds with
   * @param code stable machine-readable error code, e.g. {@code PROJECT_NOT_FOUND}
   * @param message human-readable explanation, safe to expose to clients
   */
  protected ApiException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  /** Returns the HTTP status the API responds with. */
  public HttpStatus getStatus() {
    return status;
  }

  /** Returns the stable machine-readable error code. */
  public String getCode() {
    return code;
  }
}
