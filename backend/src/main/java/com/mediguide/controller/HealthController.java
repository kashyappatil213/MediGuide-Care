package com.mediguide.controller;

import com.mediguide.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for system health verification and basic status diagnostics.
 */
@RestController
@RequestMapping({"/api/health", "/api/v1/health"})
@Tag(name = "Health & Diagnostics", description = "Endpoints for monitoring application and database connectivity")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    /**
     * Retrieves health status including application state and database connection.
     *
     * @return 200 OK with health status map
     */
    @GetMapping
    @Operation(summary = "Get System Health", description = "Verifies Spring Boot backend and PostgreSQL database status")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> status = healthService.getHealthStatus();
        return ResponseEntity.ok(status);
    }
}
