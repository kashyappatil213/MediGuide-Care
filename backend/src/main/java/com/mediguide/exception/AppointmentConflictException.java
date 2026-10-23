package com.mediguide.exception;

/**
 * Exception thrown when an appointment booking or availability operation causes
 * a domain conflict, such as doctor or patient schedule overlaps, or deactivating
 * an availability slot that has active upcoming booked appointments.
 */
public class AppointmentConflictException extends RuntimeException {

    public AppointmentConflictException(String message) {
        super(message);
    }
}
