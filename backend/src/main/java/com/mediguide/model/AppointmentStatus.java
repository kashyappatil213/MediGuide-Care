package com.mediguide.model;

/**
 * Enumeration representing the lifecycle status of an appointment.
 * Stored as a string in the database.
 */
public enum AppointmentStatus {
    BOOKED,
    CANCELLED,
    COMPLETED
}
