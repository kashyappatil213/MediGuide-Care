package com.mediguide.service;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for checking application and database health.
 */
@Service
public class HealthService {

    private final DataSource dataSource;

    public HealthService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Checks application status and verifies connectivity to PostgreSQL database.
     *
     * @return Map containing health metrics and database status
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", "MediGuide Care Backend API");
        health.put("timestamp", LocalDateTime.now().toString());

        try (Connection connection = dataSource.getConnection()) {
            boolean isDbValid = connection.isValid(2);
            health.put("database", isDbValid ? "Connected" : "Disconnected");
            health.put("databaseProduct", connection.getMetaData().getDatabaseProductName());
            health.put("databaseVersion", connection.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            health.put("database", "Error: " + e.getMessage());
        }

        return health;
    }
}
