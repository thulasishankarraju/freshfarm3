package com.example.freshfarm3.exception;

/**
 * Thrown when a request violates a business rule (e.g. duplicate review,
 * shop already approved, invalid state transition).
 * Mapped to HTTP 400 by GlobalExceptionHandler.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}