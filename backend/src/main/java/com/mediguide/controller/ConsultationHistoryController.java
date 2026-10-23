package com.mediguide.controller;

import com.mediguide.config.UserPrincipal;
import com.mediguide.dto.ConsultationHistoryResponse;
import com.mediguide.dto.ErrorResponse;
import com.mediguide.model.Role;
import com.mediguide.service.ConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for retrieving clinical consultation history.
 * Provides role-governed endpoints for patients to review their past consultations
 * and for attending doctors to review the historical clinical records of patients they have consulted.
 */
@RestController
@RequestMapping("/api/v1/consultations/history")
@Tag(name = "Consultation History", description = "Endpoints for reviewing clinical consultation histories")
@SecurityRequirement(name = "BearerAuth")
public class ConsultationHistoryController {

    private final ConsultationService consultationService;

    public ConsultationHistoryController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    /**
     * Role-aware endpoint to retrieve the authenticated user's consultation history.
     * Patients receive their own clinical consultations.
     * Doctors receive consultations they have conducted, or filter by authorized patientId.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get current user consultation history",
            description = "Retrieves consultation history for the authenticated patient or doctor, ordered newest first."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Consultation history retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConsultationHistoryResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<ConsultationHistoryResponse>> getHistory(
            @RequestParam(name = "patientId", required = false) Long patientId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getRole() == Role.PATIENT) {
            List<ConsultationHistoryResponse> history = consultationService.getPatientConsultationHistory(principal);
            return ResponseEntity.ok(history);
        }

        if (principal.getRole() == Role.DOCTOR) {
            if (patientId != null) {
                List<ConsultationHistoryResponse> history = consultationService.getPatientHistoryForDoctor(principal, patientId);
                return ResponseEntity.ok(history);
            }
            List<ConsultationHistoryResponse> history = consultationService.getDoctorConsultationHistory(principal);
            return ResponseEntity.ok(history);
        }

        // Admin fallback
        if (patientId != null) {
            List<ConsultationHistoryResponse> history = consultationService.getPatientHistoryForDoctor(principal, patientId);
            return ResponseEntity.ok(history);
        }
        return ResponseEntity.ok(List.of());
    }

    /**
     * Explicit endpoint for patients to retrieve their own consultation history.
     */
    @GetMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(
            summary = "Get patient consultation history",
            description = "Retrieves all consultation records belonging to the authenticated patient, ordered newest first."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Patient consultation history retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConsultationHistoryResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: caller does not have PATIENT role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<ConsultationHistoryResponse>> getPatientHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ConsultationHistoryResponse> history = consultationService.getPatientConsultationHistory(principal);
        return ResponseEntity.ok(history);
    }

    /**
     * Explicit endpoint for doctors to retrieve their conducted consultation history.
     */
    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Get doctor consultation history",
            description = "Retrieves all consultation records conducted by the authenticated doctor, ordered newest first."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Doctor consultation history retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConsultationHistoryResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: caller does not have DOCTOR role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<ConsultationHistoryResponse>> getDoctorHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ConsultationHistoryResponse> history = consultationService.getDoctorConsultationHistory(principal);
        return ResponseEntity.ok(history);
    }

    /**
     * Endpoint for an authorized doctor (or the patient themselves) to retrieve clinical history for a specific patient.
     * Enforces strict doctor-patient relationship authorization: doctors can only retrieve history for patients they have consulted.
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get clinical history for a specific patient",
            description = "Retrieves consultation history for a patient. Permitted for the patient themselves, or an attending doctor who has consulted this patient."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Patient clinical history retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConsultationHistoryResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (e.g. invalid patient ID)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied: caller is not an authorized doctor or matching patient",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<ConsultationHistoryResponse>> getPatientHistoryById(
            @PathVariable("patientId") Long patientId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ConsultationHistoryResponse> history = consultationService.getPatientHistoryForDoctor(principal, patientId);
        return ResponseEntity.ok(history);
    }
}
