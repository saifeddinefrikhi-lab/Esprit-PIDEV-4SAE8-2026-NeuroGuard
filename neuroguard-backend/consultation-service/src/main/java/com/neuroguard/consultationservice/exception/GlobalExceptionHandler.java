package com.neuroguard.consultationservice.exception;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
    logger.warn("Resource not found: {}", ex.getMessage());
    return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
    logger.warn("Unauthorized access: {}", ex.getMessage());
    return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
    logger.warn("Invalid argument: {}", ex.getMessage());
    return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
    logger.warn("Illegal state: {}", ex.getMessage());
    return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException ex) {
    logger.warn("Validation error: {}", ex.getMessage());
    return buildResponse(HttpStatus.BAD_REQUEST, "Erreur de validation des données");
  }

  @ExceptionHandler(FeignException.class)
  public ResponseEntity<Map<String, Object>> handleFeignException(FeignException ex) {
    logger.error("Feign client error: {} - {}", ex.status(), ex.getMessage());
    if (ex.status() == 404) {
      return buildResponse(HttpStatus.NOT_FOUND, "Service externe indisponible ou ressource non trouvée");
    }
    return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service externe indisponible");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
    logger.error("Unexpected error occurred", ex);
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne du serveur");
  }

  private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);
    return ResponseEntity.status(status).body(body);
  }
}

