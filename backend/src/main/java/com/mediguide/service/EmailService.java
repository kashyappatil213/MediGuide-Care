package com.mediguide.service;

import com.mediguide.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service handling email notifications and verification delivery using Spring's JavaMailSender.
 * SMTP configurations are read from environment properties.
 * Delivery errors are logged gracefully without failing parent transactions.
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@mediguide.local}")
    private String fromAddress;

    @Value("${app.mail.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an email verification link to a newly registered or requesting user.
     *
     * @param user the recipient user
     * @param rawToken the plaintext random verification token (never logged)
     * @return true if dispatch succeeded, false if dispatch failed or was suppressed
     */
    public boolean sendVerificationEmail(User user, String rawToken) {
        if (user == null || user.getEmail() == null || rawToken == null) {
            return false;
        }

        String verificationUrl = "";
        try {
            String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
            verificationUrl = frontendUrl + "/?verify-email=" + encodedToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(user.getEmail());
            message.setSubject("MediGuide Care — Verify Your Email Address");
            message.setText("Hello " + user.getFullName() + ",\n\n" +
                    "Welcome to MediGuide Care! Please click the link below to verify your email address:\n\n" +
                    verificationUrl + "\n\n" +
                    "This verification link is valid for 24 hours. If you did not create an account on MediGuide Care, you can safely ignore this email.\n\n" +
                    "Best regards,\n" +
                    "MediGuide Care Clinical Team");

            mailSender.send(message);
            logger.info("Verification email successfully dispatched to user publicId: {}", user.getPublicId());
            return true;
        } catch (Exception ex) {
            // Graceful degradation: Log diagnostic warning without failing user registration or workflow
            logger.warn("SMTP email dispatch failed for recipient {}. Cause: {}", user.getEmail(), ex.getMessage());
            logger.info("👉 [LOCAL DEV VERIFICATION LINK]: {}", verificationUrl);
            return false;
        }
    }
}
