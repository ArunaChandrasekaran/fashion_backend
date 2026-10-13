package com.fashion.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    public ApiException(HttpStatus status, String error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
