package com.mediguide.service;

import com.mediguide.dto.ResendVerificationResponse;
import com.mediguide.dto.VerifyEmailResponse;
import com.mediguide.exception.RateLimitException;
import com.mediguide.model.EmailVerificationToken;
import com.mediguide.model.User;
import com.mediguide.repository.EmailVerificationTokenRepository;
import com.mediguide.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

/**
 * Service managing cryptographically secure email verification tokens,
 * single-use token lifecycle, rate-limited resend cooldowns, and anti-enumeration safeguards.
 */
@Service
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.mail.verification-cooldown-seconds:60}")
    private long cooldownSeconds;

    @Value("${app.mail.verification-token-expiry-hours:24}")
    private long tokenExpiryHours;

    public EmailVerificationService(UserRepository userRepository,
                                    EmailVerificationTokenRepository tokenRepository,
                                    EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    /**
     * Generates a single-use verification token, saves its cryptographic SHA-256 hash,
     * and dispatches a verification email to the user.
     *
     * @param user the recipient user entity
     * @return the raw plaintext token string (intended only for initial email dispatch / deterministic test verification)
     */
    @Transactional
    public String generateAndSendVerificationToken(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null when generating verification token");
        }

        // 1. Generate 32 bytes of cryptographic randomness (URL-safe Base64 encoded)
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // 2. Compute secure SHA-256 hash of the token
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(tokenExpiryHours, ChronoUnit.HOURS);

        // 3. Persist or update token entity associated with this user
        Optional<EmailVerificationToken> existingTokenOpt = tokenRepository.findByUserId(user.getId());
        EmailVerificationToken tokenEntity;
        if (existingTokenOpt.isPresent()) {
            tokenEntity = existingTokenOpt.get();
            tokenEntity.setTokenHash(tokenHash);
            tokenEntity.setExpiresAt(expiresAt);
            tokenEntity.setLastSentAt(Instant.now());
        } else {
            tokenEntity = new EmailVerificationToken(user, tokenHash, expiresAt);
        }
        tokenRepository.save(tokenEntity);

        // 4. Dispatch verification email via EmailService
        emailService.sendVerificationEmail(user, rawToken);

        logger.info("Generated new email verification token for user publicId: {}", user.getPublicId());
        return rawToken;
    }

    /**
     * Verifies an email address using a provided raw plaintext token.
     * Enforces single-use consumption and expiration verification.
     *
     * @param rawToken the plaintext token string submitted by client
     * @return VerifyEmailResponse confirming successful verification
     */
    @Transactional
    public VerifyEmailResponse verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification token cannot be blank");
        }

        String tokenHash = hashToken(rawToken.trim());
        EmailVerificationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or unrecognized verification token."));

        if (token.isExpired()) {
            tokenRepository.delete(token);
            throw new IllegalStateException("Verification token has expired. Please request a new verification link.");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        // Enforce single-use: delete the verification token immediately upon success
        tokenRepository.delete(token);

        logger.info("Email successfully verified for user publicId: {}", user.getPublicId());
        return new VerifyEmailResponse("Email verified successfully.", true, user.getEmail());
    }

    /**
     * Requests a new email verification link with rate-limiting / cooldown enforcement.
     * Implements uniform response semantics to prevent email enumeration attacks.
     *
     * @param email recipient email address
     * @return ResendVerificationResponse with neutral success messaging
     */
    @Transactional
    public ResendVerificationResponse resendVerification(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        // Uniform response if user is missing or already verified (prevents email enumeration)
        String genericMessage = "If an unverified account exists for that email, a verification link has been sent.";

        if (userOpt.isEmpty() || userOpt.get().isEmailVerified()) {
            logger.info("Resend verification requested for non-existent or already verified email: {}", normalizedEmail);
            return new ResendVerificationResponse(genericMessage);
        }

        User user = userOpt.get();

        // Enforce cooldown if a token was recently dispatched
        Optional<EmailVerificationToken> existingTokenOpt = tokenRepository.findByUserId(user.getId());
        if (existingTokenOpt.isPresent()) {
            EmailVerificationToken existingToken = existingTokenOpt.get();
            Instant cooldownThreshold = existingToken.getLastSentAt().plusSeconds(cooldownSeconds);
            if (cooldownThreshold.isAfter(Instant.now())) {
                long waitSeconds = Math.max(1, Duration.between(Instant.now(), cooldownThreshold).toSeconds());
                logger.warn("Resend verification rejected due to active cooldown for user publicId: {}. Remaining: {}s",
                        user.getPublicId(), waitSeconds);
                throw new RateLimitException("Please wait " + waitSeconds + " seconds before requesting another verification email.", waitSeconds);
            }
        }

        // Generate and send fresh token
        generateAndSendVerificationToken(user);

        return new ResendVerificationResponse(genericMessage);
    }

    /**
     * Computes the SHA-256 hash of a raw token string as a lowercase hex string.
     *
     * @param rawToken plaintext token
     * @return 64-character lowercase hex string
     */
    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(64);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 message digest algorithm not available", e);
        }
    }
}
