package com.mediguide.service;

import com.mediguide.model.Role;
import com.mediguide.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String TEST_SECRET = "test-secret-key-at-least-32-bytes-long-for-hs256-signing-1234567890";
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, 15);
        jwtService.init();
    }

    @Test
    @DisplayName("Should generate a valid token and parse expected non-sensitive claims")
    void shouldGenerateAndParseValidToken() {
        User user = new User("Test Doctor", "doctor@mediguide.local", "hashed_pass", Role.DOCTOR);
        user.setId(42L);

        String token = jwtService.generateToken(user);
        assertNotNull(token);

        Claims claims = jwtService.parseAndValidateToken(token);
        assertEquals("doctor@mediguide.local", claims.getSubject());
        assertEquals("DOCTOR", claims.get("role", String.class));
        assertEquals(42, claims.get("userId", Integer.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    @DisplayName("JWT claims must not contain password or hash properties")
    void tokenClaimsMustNotContainPasswordOrHash() {
        User user = new User("Test Patient", "patient@mediguide.local", "secret_hash", Role.PATIENT);
        user.setId(77L);

        String token = jwtService.generateToken(user);
        Claims claims = jwtService.parseAndValidateToken(token);

        assertFalse(claims.containsKey("password"), "Claims must never contain password");
        assertFalse(claims.containsKey("passwordHash"), "Claims must never contain passwordHash");
        assertFalse(claims.containsKey("hash"), "Claims must never contain hash");
    }

    @Test
    @DisplayName("Should reject tampered or modified token signature")
    void shouldRejectTamperedToken() {
        User user = new User("Dr. Aisha", "aisha@mediguide.local", "hashed_pass", Role.DOCTOR);
        user.setId(10L);

        String validToken = jwtService.generateToken(user);

        // Tamper with the token string
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "abcde";

        assertThrows(JwtException.class, () -> jwtService.parseAndValidateToken(tamperedToken));
    }

    @Test
    @DisplayName("Should reject expired token deterministically")
    void shouldRejectExpiredTokenDeterministically() {
        User user = new User("Patient Expired", "expired@mediguide.local", "hashed_pass", Role.PATIENT);
        user.setId(99L);

        Instant issuedAt = Instant.now().minusSeconds(1200);
        Instant expiredAt = Instant.now().minusSeconds(300);

        String expiredToken = jwtService.generateToken(user, issuedAt, expiredAt);

        assertThrows(ExpiredJwtException.class, () -> jwtService.parseAndValidateToken(expiredToken));
    }

    @Test
    @DisplayName("Should fail fast on initialization if secret is missing or under 32 characters")
    void shouldFailInitializationIfSecretIsMissingOrTooShort() {
        JwtService emptySecretService = new JwtService("", 15);
        assertThrows(IllegalStateException.class, emptySecretService::init);

        JwtService shortSecretService = new JwtService("too-short-secret-key", 15);
        assertThrows(IllegalStateException.class, shortSecretService::init);
    }
}
