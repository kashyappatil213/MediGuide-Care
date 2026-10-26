package com.mediguide.service;

import com.mediguide.dto.ResendVerificationResponse;
import com.mediguide.dto.VerifyEmailResponse;
import com.mediguide.exception.RateLimitException;
import com.mediguide.model.EmailVerificationToken;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import com.mediguide.repository.EmailVerificationTokenRepository;
import com.mediguide.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    private EmailVerificationService verificationService;

    private User testPatient;

    @BeforeEach
    void setUp() {
        verificationService = new EmailVerificationService(userRepository, tokenRepository, emailService);
        ReflectionTestUtils.setField(verificationService, "cooldownSeconds", 60L);
        ReflectionTestUtils.setField(verificationService, "tokenExpiryHours", 24L);

        testPatient = new User("Jane Doe", "jane@example.com", "HashedPass123", Role.PATIENT, "PAT-112233");
        testPatient.setId(10L);
        testPatient.setEmailVerified(false);
    }

    @Test
    @DisplayName("Should generate secure token, store only SHA-256 hash, and dispatch verification email")
    void shouldGenerateAndSendVerificationTokenSuccessfully() {
        when(tokenRepository.findByUserId(10L)).thenReturn(Optional.empty());

        String rawToken = verificationService.generateAndSendVerificationToken(testPatient);

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());

        // Verify that entity saved to repository stores the SHA-256 hash and NOT the raw token
        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());

        EmailVerificationToken savedToken = captor.getValue();
        assertEquals(testPatient, savedToken.getUser());
        assertEquals(64, savedToken.getTokenHash().length(), "SHA-256 hex hash should be 64 characters");
        assertNotEquals(rawToken, savedToken.getTokenHash(), "Raw token must never be stored directly in plaintext");
        assertEquals(EmailVerificationService.hashToken(rawToken), savedToken.getTokenHash());

        // Verify email service was invoked with user and raw token for delivery
        verify(emailService).sendVerificationEmail(eq(testPatient), eq(rawToken));
    }

    @Test
    @DisplayName("Should verify email successfully, mark user verified, and delete single-use token")
    void shouldVerifyEmailSuccessfullyWhenTokenIsValid() {
        String rawToken = "sample-secure-token-1234567890abcdef";
        String tokenHash = EmailVerificationService.hashToken(rawToken);
        Instant futureExpiry = Instant.now().plus(24, ChronoUnit.HOURS);
        EmailVerificationToken tokenEntity = new EmailVerificationToken(testPatient, tokenHash, futureExpiry);

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

        VerifyEmailResponse response = verificationService.verifyEmail(rawToken);

        assertNotNull(response);
        assertTrue(response.isVerified());
        assertEquals("jane@example.com", response.getEmail());

        // Verify user emailVerified was set to true and saved
        assertTrue(testPatient.isEmailVerified());
        verify(userRepository).save(testPatient);

        // Verify single-use token was deleted from the repository
        verify(tokenRepository).delete(tokenEntity);
    }

    @Test
    @DisplayName("Should reject verification when token does not exist in repository")
    void shouldRejectVerificationWhenTokenIsNotFound() {
        String rawToken = "non-existent-token";
        String tokenHash = EmailVerificationService.hashToken(rawToken);
        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                verificationService.verifyEmail(rawToken));
        assertTrue(ex.getMessage().contains("Invalid or unrecognized"));
        assertFalse(testPatient.isEmailVerified());
        verify(tokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should reject verification when token is expired and delete expired token")
    void shouldRejectVerificationWhenTokenIsExpiredAndCleanup() {
        String rawToken = "expired-token-123";
        String tokenHash = EmailVerificationService.hashToken(rawToken);
        Instant pastExpiry = Instant.now().minus(1, ChronoUnit.HOURS);
        EmailVerificationToken tokenEntity = new EmailVerificationToken(testPatient, tokenHash, pastExpiry);

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(tokenEntity));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                verificationService.verifyEmail(rawToken));
        assertTrue(ex.getMessage().contains("expired"));
        assertFalse(testPatient.isEmailVerified());

        // Token should be cleaned up immediately upon discovering it is expired
        verify(tokenRepository).delete(tokenEntity);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject blank verification token")
    void shouldRejectBlankToken() {
        assertThrows(IllegalArgumentException.class, () -> verificationService.verifyEmail("   "));
        assertThrows(IllegalArgumentException.class, () -> verificationService.verifyEmail(null));
    }

    @Test
    @DisplayName("Should enforce resend cooldown and throw RateLimitException if requested too soon")
    void shouldThrowRateLimitExceptionWhenResendWithinCooldown() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(testPatient));

        EmailVerificationToken tokenEntity = new EmailVerificationToken(
                testPatient,
                "some-hash",
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        // Token was sent 10 seconds ago (cooldown is 60s)
        tokenEntity.setLastSentAt(Instant.now().minus(10, ChronoUnit.SECONDS));

        when(tokenRepository.findByUserId(10L)).thenReturn(Optional.of(tokenEntity));

        RateLimitException ex = assertThrows(RateLimitException.class, () ->
                verificationService.resendVerification("jane@example.com"));

        assertTrue(ex.getRetryAfterSeconds() > 0);
        verify(emailService, never()).sendVerificationEmail(any(), any());
    }

    @Test
    @DisplayName("Should allow resend verification when cooldown threshold has elapsed")
    void shouldResendSuccessfullyWhenCooldownExpired() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(testPatient));

        EmailVerificationToken tokenEntity = new EmailVerificationToken(
                testPatient,
                "some-old-hash",
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        // Token was sent 70 seconds ago (cooldown is 60s)
        tokenEntity.setLastSentAt(Instant.now().minus(70, ChronoUnit.SECONDS));

        when(tokenRepository.findByUserId(10L)).thenReturn(Optional.of(tokenEntity));

        ResendVerificationResponse response = verificationService.resendVerification("jane@example.com");

        assertNotNull(response);
        assertTrue(response.getMessage().contains("If an unverified account exists"));
        verify(emailService).sendVerificationEmail(eq(testPatient), any(String.class));
    }

    @Test
    @DisplayName("Should return uniform anti-enumeration response when resending for non-existent email")
    void shouldReturnUniformResponseWhenUserDoesNotExistOnResend() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        ResendVerificationResponse response = verificationService.resendVerification("ghost@example.com");

        assertNotNull(response);
        assertEquals("If an unverified account exists for that email, a verification link has been sent.",
                response.getMessage());
        verify(emailService, never()).sendVerificationEmail(any(), any());
    }

    @Test
    @DisplayName("Should return uniform anti-enumeration response when resending for already verified user")
    void shouldReturnUniformResponseWhenUserIsAlreadyVerifiedOnResend() {
        testPatient.setEmailVerified(true);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(testPatient));

        ResendVerificationResponse response = verificationService.resendVerification("jane@example.com");

        assertNotNull(response);
        assertEquals("If an unverified account exists for that email, a verification link has been sent.",
                response.getMessage());
        verify(emailService, never()).sendVerificationEmail(any(), any());
    }
}
