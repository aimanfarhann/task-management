package com.taskflow.common.exception;

import java.util.List;

/**
 * The single JSON error shape of the API contract: {@code { code, message, fieldErrors }}. {@code
 * fieldErrors} is always present and empty unless the error is a validation failure.
 *
 * @param code stable machine-readable error code
 * @param message human-readable explanation
 * @param fieldErrors per-field validation failures, empty for non-validation errors
 */
public record ErrorResponse(String code, String message, List<FieldErrorDto> fieldErrors) {

  /**
   * A single field validation failure.
   *
   * @param field name of the offending request field
   * @param message what is wrong with the value
   */
  public record FieldErrorDto(String field, String message) {}

  /**
   * Creates an error response without field errors.
   *
   * @param code stable machine-readable error code
   * @param message human-readable explanation
   * @return the error response with an empty {@code fieldErrors} list
   */
  public static ErrorResponse of(String code, String message) {
    return new ErrorResponse(code, message, List.of());
  }
}
