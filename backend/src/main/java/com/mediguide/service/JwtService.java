package com.mediguide.service;

import com.mediguide.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Service responsible for creating, signing, and validating JWT access tokens.
 *
 * IMPORTANT SECURITY NOTE:
 * Signed JWTs (JWS) are cryptographically signed to guarantee authenticity and integrity,
 * but they are NOT encrypted. The payload is standard Base64URL-encoded JSON. Anyone with
 * access to the token can decode and view its claims. Therefore:
 * 1. Passwords, password hashes, and database internal IDs that are secret must never be included.
 * 2. Sensitive clinical records, patient diagnoses, or prescription notes must NEVER be stored in JWT claims.
 */
@Service
public class JwtService {

    private final String secret;
    private final long expirationMinutes;
    private SecretKey signingKey;

    public JwtService(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration-minutes:15}") long expirationMinutes) {
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Verifies that the configured JWT signing secret meets minimum security standards (256 bits / 32 bytes).
     * Fails fast at startup if missing or insufficient.
     */
    @PostConstruct
    public void init() {
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalStateException(
                    "JWT signing secret is missing or insufficient. " +
                    "Please configure the JWT_SECRET environment variable with at least 32 characters (256 bits)."
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed JWT access token for an authenticated user with default lifetime.
     *
     * @param user authenticated user
     * @return signed JWT string
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(expirationMinutes));
        return generateToken(user, now, expiry);
    }

    /**
     * Generates a signed JWT access token with explicit timestamps (used for deterministic expiry tests).
     *
     * @param user authenticated user
     * @param issuedAt issuance timestamp
     * @param expiresAt expiration timestamp
     * @return signed JWT string
     */
    public String generateToken(User user, Instant issuedAt, Instant expiresAt) {
        if (signingKey == null) {
            init();
        }

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates the cryptographic signature and expiration of a JWT access token.
     *
     * @param token signed JWT token string
     * @return claims payload if signature and expiration are valid
     * @throws JwtException if signature is invalid, token is expired, or malformed
     */
    public Claims parseAndValidateToken(String token) {
        if (signingKey == null) {
            init();
        }

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns the configured token lifetime in seconds.
     *
     * @return duration in seconds
     */
    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}
