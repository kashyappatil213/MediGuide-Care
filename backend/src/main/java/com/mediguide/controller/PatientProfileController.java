package com.mediguide.controller;

import com.mediguide.config.UserPrincipal;
import com.mediguide.dto.ErrorResponse;
import com.mediguide.dto.PatientProfileRequest;
import com.mediguide.dto.PatientProfileResponse;
import com.mediguide.service.PatientProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authenticated patient profile and contact management.
 */
@RestController
@RequestMapping("/api/v1/patient/profile")
@Tag(name = "Patient Profile", description = "Endpoints for patient personal contact detail management")
@SecurityRequirement(name = "BearerAuth")
public class PatientProfileController {

    private final PatientProfileService patientProfileService;

    public PatientProfileController(PatientProfileService patientProfileService) {
        this.patientProfileService = patientProfileService;
    }

    /**
     * Retrieves the authenticated patient's profile details.
     *
     * @param principal authenticated patient user principal
     * @return 200 OK with PatientProfileResponse
     */
    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(
            summary = "Get current patient profile",
            description = "Retrieves the authenticated patient's personal contact details. Requires ROLE_PATIENT authority."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Patient profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PatientProfileResponse.class))
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
    public ResponseEntity<PatientProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        PatientProfileResponse response = patientProfileService.getPatientProfile(principal.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the authenticated patient's contact phone number.
     *
     * @param request validated profile details
     * @param principal authenticated patient user principal
     * @return 200 OK with updated PatientProfileResponse
     */
    @PutMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(
            summary = "Update current patient profile",
            description = "Updates the authenticated patient's contact phone number. Requires ROLE_PATIENT authority."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Patient profile updated successfully",
                    content = @Content(schema = @Schema(implementation = PatientProfileResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failure or invalid phone format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
    public ResponseEntity<PatientProfileResponse> updateMyProfile(
            @Valid @RequestBody PatientProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        PatientProfileResponse response = patientProfileService.updatePatientProfile(principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(
            summary = "Create or update patient profile (POST alias)",
            description = "Updates the authenticated patient's contact details. Requires ROLE_PATIENT authority."
    )
    public ResponseEntity<PatientProfileResponse> createOrUpdateMyProfile(
            @Valid @RequestBody PatientProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return updateMyProfile(request, principal);
    }
}
