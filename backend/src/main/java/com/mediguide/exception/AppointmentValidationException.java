package com.mediguide.exception;

/**
 * Exception thrown when appointment scheduling or availability input violates
 * business domain constraints (e.g., past timestamps, inverted intervals, or times
 * falling outside doctor availability).
 */
public class AppointmentValidationException extends RuntimeException {

    public AppointmentValidationException(String message) {
        super(message);
    }
}
