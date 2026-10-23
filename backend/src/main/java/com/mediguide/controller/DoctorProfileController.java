package com.mediguide.controller;

import com.mediguide.config.UserPrincipal;
import com.mediguide.dto.DoctorProfileRequest;
import com.mediguide.dto.DoctorProfileResponse;
import com.mediguide.dto.ErrorResponse;
import com.mediguide.dto.PublicDoctorProfileResponse;
import com.mediguide.service.DoctorProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for doctor clinic profile management and patient doctor discovery.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Doctor Profiles", description = "Endpoints for doctor clinic profile configuration and patient doctor search")
@SecurityRequirement(name = "BearerAuth")
public class DoctorProfileController {

    private final DoctorProfileService doctorProfileService;

    public DoctorProfileController(DoctorProfileService doctorProfileService) {
        this.doctorProfileService = doctorProfileService;
    }

    /**
     * Retrieves the authenticated doctor's profile.
     *
     * @param principal authenticated doctor user principal
     * @return 200 OK with DoctorProfileResponse
     */
    @GetMapping("/doctor/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Get current doctor profile",
            description = "Retrieves the authenticated doctor's clinical and clinic location profile. Requires ROLE_DOCTOR authority."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Doctor profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DoctorProfileResponse.class))
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
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Doctor profile has not been configured yet",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<DoctorProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        DoctorProfileResponse response = doctorProfileService.getMyProfile(principal.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Creates or updates the authenticated doctor's profile.
     * Uses the authenticated principal exclusively to ensure doctors can only edit their own profile.
     *
     * @param request validated profile details
     * @param principal authenticated doctor user principal
     * @return 200 OK with updated DoctorProfileResponse
     */
    @PutMapping("/doctor/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Update current doctor profile",
            description = "Creates or updates the authenticated doctor's clinical profile. Requires ROLE_DOCTOR authority."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Doctor profile saved successfully",
                    content = @Content(schema = @Schema(implementation = DoctorProfileResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failure or invalid PIN code format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
    public ResponseEntity<DoctorProfileResponse> updateMyProfile(
            @Valid @RequestBody DoctorProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        DoctorProfileResponse response = doctorProfileService.createOrUpdateProfile(principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/doctor/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Create or update current doctor profile (POST alias)",
            description = "Creates or updates the authenticated doctor's clinical profile. Requires ROLE_DOCTOR authority."
    )
    public ResponseEntity<DoctorProfileResponse> createOrUpdateMyProfile(
            @Valid @RequestBody DoctorProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return updateMyProfile(request, principal);
    }

    /**
     * Searches published doctor profiles by specialty, city, area, and PIN code.
     * Filters are combined with AND semantics. Text fields are case-insensitive.
     * Returns sanitized public profile DTOs without sensitive credentials or numeric database IDs.
     *
     * @param specialty optional medical specialty filter
     * @param city optional city filter
     * @param area optional locality/area filter
     * @param pinCode optional PIN code filter (exact match)
     * @param page zero-based page index (default 0)
     * @param size page size (default 20, max 50)
     * @return 200 OK with list of matching PublicDoctorProfileResponse
     */
    @GetMapping("/doctors/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Search published doctor profiles",
            description = "Searches published doctor clinic profiles using optional specialty, city, area, and PIN code filters. Accessible to authenticated users."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Matching doctor profiles retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PublicDoctorProfileResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<PublicDoctorProfileResponse>> searchDoctors(
            @RequestParam(value = "specialty", required = false) String specialty,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "area", required = false) String area,
            @RequestParam(value = "pinCode", required = false) String pinCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        List<PublicDoctorProfileResponse> results = doctorProfileService.searchPublicProfiles(
                specialty, city, area, pinCode, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Retrieves a single doctor's published profile by their public MediGuide ID.
     *
     * @param publicId public identifier (e.g. DOC-XXXXXX)
     * @return 200 OK with PublicDoctorProfileResponse
     */
    @GetMapping("/doctors/{publicId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get public doctor profile by public ID",
            description = "Retrieves public clinic and specialty profile details for a doctor using their public ID. Accessible to authenticated users."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Doctor profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PublicDoctorProfileResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid public ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Full authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Doctor profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<PublicDoctorProfileResponse> getDoctorByPublicId(
            @PathVariable("publicId") String publicId) {
        PublicDoctorProfileResponse response = doctorProfileService.getPublicProfileByPublicId(publicId);
        return ResponseEntity.ok(response);
    }
}
