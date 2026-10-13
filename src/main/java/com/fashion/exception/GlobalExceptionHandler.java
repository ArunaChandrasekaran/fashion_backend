package com.fashion.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return build(ex.getStatus(), ex.getError(), ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Please correct the highlighted fields", fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> fields.put(v.getPropertyPath().toString(), v.getMessage()));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Please correct the highlighted fields", fields);
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ErrorResponse> handleAuth(Exception ex) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid email or password", Map.of());
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled() {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "This account has been deactivated", Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleDenied() {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action", Map.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock() {
        return build(HttpStatus.CONFLICT, "CONFLICT", "This item was just updated. Please try again.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex) {
        log.error("Unhandled error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", Map.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message, Map<String, String> fields) {
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), error, message, fields);
        return ResponseEntity.status(status).body(body);
    }
}
