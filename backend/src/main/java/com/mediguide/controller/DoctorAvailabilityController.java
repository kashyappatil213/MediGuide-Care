package com.mediguide.controller;

import com.mediguide.config.UserPrincipal;
import com.mediguide.dto.AvailabilityResponse;
import com.mediguide.dto.BatchAvailabilityResponse;
import com.mediguide.dto.CreateAvailabilityRequest;
import com.mediguide.dto.CreateBatchAvailabilityRequest;
import com.mediguide.dto.ErrorResponse;
import com.mediguide.service.DoctorAvailabilityService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for doctor availability management.
 * Provides endpoints for creating availability slots, listing the authenticated doctor's
 * availability, and deactivating existing slots with conflict protection.
 */
@RestController
@RequestMapping("/api/v1/doctor/availabilities")
@Tag(name = "Doctor Availability", description = "Endpoints for doctor availability scheduling and management")
@SecurityRequirement(name = "BearerAuth")
public class DoctorAvailabilityController {

    private final DoctorAvailabilityService availabilityService;

    public DoctorAvailabilityController(DoctorAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /**
     * Creates a new availability slot for the authenticated doctor.
     *
     * @param principal authenticated doctor user principal
     * @param request validated availability creation details
     * @return 201 Created with AvailabilityResponse
     */
    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Create doctor availability",
            description = "Creates a new availability time slot for the authenticated doctor. Requires ROLE_DOCTOR authority."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Availability slot created successfully",
                    content = @Content(schema = @Schema(implementation = AvailabilityResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed, missing fields, inverted times, or past interval",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: requires DOCTOR role or account mismatch",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<AvailabilityResponse> createAvailability(
            @Valid @RequestBody CreateAvailabilityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AvailabilityResponse response = availabilityService.createAvailability(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Pre-schedules multiple availability time slots for the authenticated doctor in a single batch.
     * Validates each slot individually, checks for overlaps, and returns a detailed batch response
     * showing created slots and rejected slots with specific reasons.
     *
     * @param request validated batch availability request with list of slots
     * @param principal authenticated doctor user principal
     * @return 200 OK with BatchAvailabilityResponse detailing successful and rejected slots
     */
    @PostMapping("/batch")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Pre-schedule multiple doctor availability slots",
            description = "Creates multiple availability time slots in a single batch request for the authenticated doctor. Requires ROLE_DOCTOR authority."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Batch processed successfully, returning created and rejected slot details",
                    content = @Content(schema = @Schema(implementation = BatchAvailabilityResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed: empty list or malformed body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: requires DOCTOR role or account mismatch",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<BatchAvailabilityResponse> createBatchAvailability(
            @Valid @RequestBody CreateBatchAvailabilityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        BatchAvailabilityResponse response = availabilityService.createBatchAvailability(principal, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all availability slots for the currently authenticated doctor, sorted chronologically.
     * Does not accept or allow querying other doctors' IDs.
     *
     * @param principal authenticated doctor user principal
     * @return 200 OK with list of availability responses
     */
    @GetMapping
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "List authenticated doctor availabilities",
            description = "Retrieves all availability slots registered for the authenticated doctor, sorted chronologically. Requires ROLE_DOCTOR authority."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Availability slots retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AvailabilityResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: requires DOCTOR role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<AvailabilityResponse>> getMyAvailabilities(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<AvailabilityResponse> responses = availabilityService.getAvailabilitiesByDoctor(principal.getId());
        return ResponseEntity.ok(responses);
    }

    /**
     * Deactivates an existing availability slot belonging to the authenticated doctor.
     * Rejects deactivation with 409 Conflict if any future booked appointments depend on this slot.
     *
     * @param id primary key of the availability slot to deactivate
     * @param principal authenticated doctor user principal
     * @return 200 OK with updated AvailabilityResponse (active=false)
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Deactivate doctor availability slot",
            description = "Deactivates an availability slot. Fails with 409 Conflict if future booked appointments depend on it. Requires slot ownership and ROLE_DOCTOR authority."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Availability slot deactivated successfully",
                    content = @Content(schema = @Schema(implementation = AvailabilityResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid availability slot ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: caller does not own the availability slot or wrong role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Doctor availability slot not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: future booked appointments depend on this schedule",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<AvailabilityResponse> deactivateAvailability(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AvailabilityResponse response = availabilityService.deactivateAvailability(principal, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Discovers all active future availability slots across all doctors eligible for booking,
     * sorted chronologically by start time.
     * Excludes inactive slots and slots whose end time has passed.
     *
     * @return 200 OK with list of active future availability responses
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(
            summary = "Discover active doctor availability slots",
            description = "Retrieves all active future doctor availability slots eligible for booking across all doctors, sorted chronologically. Excludes inactive slots and slots whose end time is in the past. Requires ROLE_PATIENT authority."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Active availability slots retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AvailabilityResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: requires PATIENT role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<AvailabilityResponse>> getActiveAvailabilities() {
        List<AvailabilityResponse> responses = availabilityService.getActiveFutureAvailabilities();
        return ResponseEntity.ok(responses);
    }
}
