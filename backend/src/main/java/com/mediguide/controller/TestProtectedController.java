package com.mediguide.controller;

import com.mediguide.config.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Temporary development test controller used exclusively to verify JWT authentication
 * and role-based authorization filters.
 *
 * NOTE: This is a diagnostic endpoint for Phase 2 verification and does not contain
 * clinical business logic. It should be removed or disabled in production environments.
 */
@RestController
@RequestMapping("/api/v1/test")
@Tag(name = "Authentication Verification (Dev/Test)", description = "Temporary test endpoints to verify JWT security and roles")
public class TestProtectedController {

    /**
     * Minimal protected endpoint accessible to any authenticated user with a valid JWT.
     *
     * @param principal the authenticated user principal extracted from JWT claims
     * @return 200 OK with confirmation and principal details
     */
    @GetMapping("/protected")
    @Operation(summary = "Test Protected Endpoint", description = "Verifies valid JWT token authentication for any authenticated role")
    public ResponseEntity<Map<String, Object>> getProtectedInfo(@AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "authenticated");
        response.put("message", "Access granted to protected test endpoint");
        response.put("userId", principal != null ? principal.getId() : null);
        response.put("email", principal != null ? principal.getEmail() : null);
        response.put("role", principal != null && principal.getRole() != null ? principal.getRole().name() : null);
        return ResponseEntity.ok(response);
    }

    /**
     * Role-restricted test endpoint accessible only to users with the DOCTOR role.
     *
     * @param principal the authenticated doctor principal
     * @return 200 OK with confirmation
     */
    @GetMapping("/doctor-only")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Test Doctor-Only Endpoint", description = "Verifies role-based restriction: only accessible with ROLE_DOCTOR authority")
    public ResponseEntity<Map<String, Object>> getDoctorOnlyInfo(@AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "authenticated");
        response.put("message", "Access granted to doctor-only test endpoint");
        response.put("userId", principal != null ? principal.getId() : null);
        response.put("email", principal != null ? principal.getEmail() : null);
        response.put("role", principal != null && principal.getRole() != null ? principal.getRole().name() : null);
        return ResponseEntity.ok(response);
    }
}
