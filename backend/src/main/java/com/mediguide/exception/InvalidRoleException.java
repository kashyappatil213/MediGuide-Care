package com.mediguide.exception;

/**
 * Exception thrown when an unauthorized or unsupported role is requested for registration.
 */
public class InvalidRoleException extends RuntimeException {

    public InvalidRoleException(String message) {
        super(message);
    }
}
