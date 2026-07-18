package com.taskflow.common.exception;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps every exception to the single API error shape (ARCHITECTURE.md §3.4). Controllers never
 * try/catch; services throw {@link ApiException} subclasses and this advice translates them. No
 * stack traces or SQL ever leak into responses (RULES.md §31).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Translates domain exceptions into their declared status and code.
   *
   * @param exception the thrown domain exception
   * @return the contract error body with the exception's status
   */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
    log.info("Domain error {}: {}", exception.getCode(), exception.getMessage());
    return ResponseEntity.status(exception.getStatus())
        .body(ErrorResponse.of(exception.getCode(), exception.getMessage()));
  }

  /**
   * Translates Bean Validation failures into 400 with per-field errors.
   *
   * @param exception the binding failure raised by {@code @Valid}
   * @return the contract error body listing each invalid field
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationFailure(
      MethodArgumentNotValidException exception) {
    List<ErrorResponse.FieldErrorDto> fieldErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    new ErrorResponse.FieldErrorDto(error.getField(), error.getDefaultMessage()))
            .toList();
    return ResponseEntity.badRequest()
        .body(new ErrorResponse("VALIDATION_FAILED", "Request validation failed", fieldErrors));
  }

  /**
   * Translates unreadable request bodies (missing body, malformed JSON, wrong enum value) into 400.
   *
   * @param exception the message conversion failure
   * @return the contract error body with code {@code MALFORMED_REQUEST}
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableBody(
      HttpMessageNotReadableException exception) {
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of("MALFORMED_REQUEST", "Request body is missing or malformed"));
  }

  /**
   * Translates path/query parameter type mismatches (e.g. non-numeric id) into 400.
   *
   * @param exception the parameter conversion failure
   * @return the contract error body naming the offending parameter
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception) {
    return ResponseEntity.badRequest()
        .body(
            ErrorResponse.of(
                "INVALID_PARAMETER",
                "Parameter '" + exception.getName() + "' has an invalid value"));
  }

  /**
   * Translates requests for unknown paths into 404 in the contract shape.
   *
   * @param exception the missing static/handler resource
   * @return the contract error body with code {@code NOT_FOUND}
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of("NOT_FOUND", "Resource not found"));
  }

  /**
   * Translates unsupported HTTP methods into 405 in the contract shape.
   *
   * @param exception the unsupported method failure
   * @return the contract error body with code {@code METHOD_NOT_ALLOWED}
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException exception) {
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(ErrorResponse.of("METHOD_NOT_ALLOWED", "HTTP method not allowed for this resource"));
  }

  /**
   * Translates Spring Security access denials raised inside the MVC layer into 403.
   *
   * @param exception the access denial
   * @return the contract error body with code {@code FORBIDDEN}
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ErrorResponse.of("FORBIDDEN", "You are not permitted to perform this action"));
  }

  /**
   * Last-resort handler: logs the full failure server-side and returns an opaque 500.
   *
   * @param exception any exception not handled above
   * @return the contract error body with code {@code INTERNAL_ERROR}
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
    log.error("Unhandled exception", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred"));
  }
}
