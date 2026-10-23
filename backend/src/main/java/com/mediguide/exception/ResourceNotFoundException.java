package com.mediguide.exception;

/**
 * Exception thrown when a requested domain entity (user, availability, appointment) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
