package com.mediguide.controller;

import com.mediguide.config.UserPrincipal;
import com.mediguide.dto.AppointmentResponse;
import com.mediguide.dto.BookAppointmentRequest;
import com.mediguide.dto.ErrorResponse;
import com.mediguide.model.Role;
import com.mediguide.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for appointment booking and consultation management.
 * Provides endpoints for scheduling consultation appointments, retrieving individual
 * appointment details with participant authorization, and listing authenticated user appointments.
 */
@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Endpoints for appointment booking, retrieval, and personal schedule listing")
@SecurityRequirement(name = "BearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Books a new consultation appointment for the authenticated patient.
     * Enforces pessimistic concurrency locking across both doctor and patient to prevent double-booking.
     *
     * @param request validated appointment booking details
     * @param principal authenticated patient user principal
     * @return 201 Created with AppointmentResponse
     */
    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(
            summary = "Book appointment",
            description = "Books a consultation appointment for the authenticated patient within an active doctor availability slot. Requires ROLE_PATIENT authority."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Appointment booked successfully",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed, past time, interval outside availability, or inactive slot",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: requires PATIENT role or account mismatch",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Doctor availability slot or user record not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: doctor or patient already has an overlapping booked appointment",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @Valid @RequestBody BookAppointmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppointmentResponse response = appointmentService.bookAppointment(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves an appointment by ID with participant access verification.
     * Only the booking patient, assigned doctor, or an admin may access the record.
     *
     * @param id primary key of the appointment
     * @param principal authenticated user principal
     * @return 200 OK with AppointmentResponse
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get appointment by ID",
            description = "Retrieves details for a specific appointment. Only authorized participants (the booking patient, assigned doctor, or admin) may view it."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Appointment details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid appointment ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: caller is not an authorized participant in this appointment",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<AppointmentResponse> getAppointmentById(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppointmentResponse response = appointmentService.getAppointmentById(principal, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all appointments for the currently authenticated user.
     * Patients receive their own booked appointments; doctors receive appointments assigned to them.
     * Unsupported roles are rejected with 403 Forbidden.
     *
     * @param principal authenticated user principal
     * @return 200 OK with list of personal appointments
     */
    @GetMapping("/my")
    @Operation(
            summary = "List authenticated user's appointments",
            description = "Retrieves appointments for the currently authenticated user. Returns personal bookings for PATIENT and assigned consultations for DOCTOR."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Appointments retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AppointmentResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: unsupported role or unauthenticated caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required to view personal appointments");
        }

        if (principal.getRole() == Role.PATIENT) {
            List<AppointmentResponse> responses = appointmentService.getAppointmentsByPatient(principal, principal.getId());
            return ResponseEntity.ok(responses);
        } else if (principal.getRole() == Role.DOCTOR) {
            List<AppointmentResponse> responses = appointmentService.getAppointmentsByDoctor(principal, principal.getId());
            return ResponseEntity.ok(responses);
        } else {
            throw new AccessDeniedException("Unsupported role for listing personal appointments: " + principal.getRole());
        }
    }

    /**
     * Cancels an existing consultation appointment.
     * Permitted only for the assigned patient or assigned doctor.
     *
     * @param id appointment primary key
     * @param principal authenticated user principal
     * @return 200 OK with updated AppointmentResponse (status CANCELLED)
     */
    @PatchMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel appointment",
            description = "Cancels an existing consultation appointment. Permitted only for the assigned patient or assigned doctor. Requires the appointment to be currently in BOOKED status."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Appointment cancelled successfully",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid appointment ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: caller is not the assigned patient or assigned doctor",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: appointment is not in BOOKED status (already CANCELLED or COMPLETED)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppointmentResponse response = appointmentService.cancelAppointment(principal, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Marks an appointment as completed.
     * Permitted only for the assigned doctor.
     *
     * @param id appointment primary key
     * @param principal authenticated user principal
     * @return 200 OK with updated AppointmentResponse (status COMPLETED)
     */
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Complete appointment",
            description = "Marks a consultation appointment as completed. Permitted only for the assigned doctor. Requires the appointment to be currently in BOOKED status."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Appointment completed successfully",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid appointment ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: caller is not the assigned doctor, or role is not DOCTOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: appointment is not in BOOKED status (already CANCELLED or COMPLETED)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<AppointmentResponse> completeAppointment(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppointmentResponse response = appointmentService.completeAppointment(principal, id);
        return ResponseEntity.ok(response);
    }
}
