package com.mediguide.exception;

/**
 * Exception thrown when attempting to register with an email that is already in use.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
