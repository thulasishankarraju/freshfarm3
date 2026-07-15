package com.example.freshfarm3.exception;

/**
 * Thrown when a requested entity (Shop, Order, Product, etc.) cannot be found.
 * Mapped to HTTP 404 by GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}