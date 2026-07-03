package com.example.freshfarm3.exception;

/**
 * Thrown when a user tries to act on a resource that does not belong to them
 * (e.g. reviewing someone else's order). Mapped to HTTP 403 by GlobalExceptionHandler.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}