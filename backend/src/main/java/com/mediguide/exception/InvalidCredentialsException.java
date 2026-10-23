package com.mediguide.exception;

/**
 * Exception thrown when user authentication fails.
 * Generic error message prevents disclosing whether a specific email is registered.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
