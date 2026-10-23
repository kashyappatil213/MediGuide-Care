package com.mediguide.controller;

import com.mediguide.config.UserPrincipal;
import com.mediguide.dto.ConsultationResponse;
import com.mediguide.dto.CreateConsultationRequest;
import com.mediguide.dto.CreatePrescriptionRequest;
import com.mediguide.dto.ErrorResponse;
import com.mediguide.dto.PrescriptionResponse;
import com.mediguide.dto.UpdateFollowUpRequest;
import com.mediguide.service.ConsultationService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for consultation records and prescription management.
 * Provides endpoints for documenting clinical diagnosis, treatment plans, and issuing prescriptions.
 */
@RestController
@RequestMapping("/api/v1/appointments/{appointmentId}")
@Tag(name = "Consultations and Prescriptions", description = "Endpoints for consultation records and prescription management")
@SecurityRequirement(name = "BearerAuth")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    /**
     * Creates a new consultation record for the specified appointment.
     * Permitted only for the assigned doctor.
     */
    @PostMapping("/consultation")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Create consultation record",
            description = "Creates a consultation record (notes/diagnosis and treatment) for an appointment. Permitted only for the assigned doctor."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Consultation record created successfully",
                    content = @Content(schema = @Schema(implementation = ConsultationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed or missing required fields",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: caller is not the assigned doctor or lacks DOCTOR role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: appointment is cancelled or consultation record already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ConsultationResponse> createConsultation(
            @PathVariable("appointmentId") Long appointmentId,
            @Valid @RequestBody CreateConsultationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ConsultationResponse response = consultationService.createConsultation(principal, appointmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing consultation record for the specified appointment.
     * Permitted only for the assigned doctor.
     */
    @PutMapping("/consultation")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Update consultation record",
            description = "Updates an existing consultation record for an appointment. Permitted only for the assigned doctor."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Consultation record updated successfully",
                    content = @Content(schema = @Schema(implementation = ConsultationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed or missing required fields",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: caller is not the assigned doctor",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Appointment or consultation record not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: appointment is cancelled",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ConsultationResponse> updateConsultation(
            @PathVariable("appointmentId") Long appointmentId,
            @Valid @RequestBody CreateConsultationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ConsultationResponse response = consultationService.updateConsultation(principal, appointmentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the recommended follow-up date and optional notes for an appointment's consultation.
     * Permitted only for the assigned doctor.
     */
    @PatchMapping("/consultation/follow-up")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Update follow-up recommendation",
            description = "Updates the recommended follow-up date and notes for an appointment's consultation. Permitted only for the assigned doctor."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Follow-up information updated successfully",
                    content = @Content(schema = @Schema(implementation = ConsultationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (e.g. past date or excessive note length)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: caller is not the assigned doctor",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Appointment or consultation record not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: appointment is cancelled",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ConsultationResponse> updateFollowUp(
            @PathVariable("appointmentId") Long appointmentId,
            @Valid @RequestBody UpdateFollowUpRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ConsultationResponse response = consultationService.updateFollowUp(principal, appointmentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the consultation record for the specified appointment.
     * Accessible by the assigned doctor, the booked patient, or an admin.
     */
    @GetMapping("/consultation")
    @Operation(
            summary = "Get consultation record",
            description = "Retrieves the consultation record for an appointment. Accessible by authorized participants (assigned doctor or booked patient)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Consultation record retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ConsultationResponse.class))
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
                    description = "Appointment or consultation record not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ConsultationResponse> getConsultation(
            @PathVariable("appointmentId") Long appointmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ConsultationResponse response = consultationService.getConsultationByAppointmentId(principal, appointmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Adds a new prescription item to the specified appointment.
     * Permitted only for the assigned doctor.
     */
    @PostMapping("/prescription")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Create prescription item",
            description = "Adds a medication prescription item to an appointment. Permitted only for the assigned doctor."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Prescription item created successfully",
                    content = @Content(schema = @Schema(implementation = PrescriptionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed or missing required fields",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: caller is not the assigned doctor",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: appointment is cancelled",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<PrescriptionResponse> createPrescription(
            @PathVariable("appointmentId") Long appointmentId,
            @Valid @RequestBody CreatePrescriptionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        PrescriptionResponse response = consultationService.createPrescription(principal, appointmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all prescription items for the specified appointment.
     * Accessible by the assigned doctor, the booked patient, or an admin.
     */
    @GetMapping("/prescription")
    @Operation(
            summary = "Get appointment prescriptions",
            description = "Retrieves all prescription items associated with an appointment. Accessible by authorized participants."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Prescriptions retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PrescriptionResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: caller is not an authorized participant",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptions(
            @PathVariable("appointmentId") Long appointmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<PrescriptionResponse> responses = consultationService.getPrescriptionsByAppointmentId(principal, appointmentId);
        return ResponseEntity.ok(responses);
    }
}
