package com.taskflow.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request cannot be authenticated — bad credentials or an invalid refresh token. Maps
 * to HTTP 401.
 */
public class UnauthorizedException extends ApiException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates an unauthorized exception.
   *
   * @param code error code, e.g. {@code INVALID_CREDENTIALS} or {@code INVALID_REFRESH_TOKEN}
   * @param message human-readable explanation, safe to expose to clients
   */
  public UnauthorizedException(String code, String message) {
    super(HttpStatus.UNAUTHORIZED, code, message);
  }
}
