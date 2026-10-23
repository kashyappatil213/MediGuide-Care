package com.mediguide.controller;

import com.mediguide.config.UserPrincipal;
import com.mediguide.dto.ClinicalHistorySummaryRequest;
import com.mediguide.dto.ClinicalHistorySummaryResponse;
import com.mediguide.dto.ErrorResponse;
import com.mediguide.service.ClinicalHistorySummaryService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for generating AI-assisted clinical history summaries.
 * Accessible exclusively to attending doctors for authorized patients they have consulted.
 */
@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Clinical Summarizer", description = "AI-assisted clinical history summarization for doctors")
@SecurityRequirement(name = "BearerAuth")
public class ClinicalHistorySummaryController {

    private final ClinicalHistorySummaryService summaryService;

    public ClinicalHistorySummaryController(ClinicalHistorySummaryService summaryService) {
        this.summaryService = summaryService;
    }

    /**
     * Generates a concise, structured AI summary of an authorized patient's recorded clinical history.
     *
     * @param request payload containing the target patient ID
     * @param principal authenticated doctor user principal
     * @return ClinicalHistorySummaryResponse draft
     */
    @PostMapping("/clinical-history-summary")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Generate AI clinical history summary",
            description = "Summarizes authorized consultation records for a patient seen by the authenticated doctor."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Clinical history summary draft generated successfully",
                    content = @Content(schema = @Schema(implementation = ClinicalHistorySummaryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failure or no consultation history recorded for this patient",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: caller is not an authorized doctor for this patient",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "External AI provider failure, rate limit, or timeout",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "AI provider is unconfigured or unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ClinicalHistorySummaryResponse> generateSummary(
            @Valid @RequestBody ClinicalHistorySummaryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ClinicalHistorySummaryResponse response = summaryService.summarizePatientHistory(principal, request.getPatientId());
        return ResponseEntity.ok(response);
    }
}
