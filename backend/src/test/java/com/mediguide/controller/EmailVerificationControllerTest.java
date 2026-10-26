package com.mediguide.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediguide.dto.BookAppointmentRequest;
import com.mediguide.dto.CreateAvailabilityRequest;
import com.mediguide.dto.LoginRequest;
import com.mediguide.dto.RegisterRequest;
import com.mediguide.dto.ResendVerificationRequest;
import com.mediguide.dto.VerifyEmailRequest;
import com.mediguide.model.DoctorAvailability;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import com.mediguide.repository.DoctorAvailabilityRepository;
import com.mediguide.repository.UserRepository;
import com.mediguide.service.EmailVerificationService;
import com.mediguide.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmailVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorAvailabilityRepository availabilityRepository;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("1. New registration sets emailVerified=false and does NOT leak verification token in response")
    void shouldRegisterUserAsUnverifiedAndNotExposeTokens() throws Exception {
        String email = "unverified." + System.nanoTime() + "@mediguide.local";
        RegisterRequest request = new RegisterRequest("Test Patient", email, "Password123!", Role.PATIENT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.emailVerified", is(false)))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.rawToken").doesNotExist())
                .andExpect(jsonPath("$.tokenHash").doesNotExist())
                .andExpect(jsonPath("$.verificationToken").doesNotExist());

        User saved = userRepository.findByEmail(email).orElseThrow();
        assertFalse(saved.isEmailVerified(), "New registered user must be unverified initially");
    }

    @Test
    @DisplayName("2. Unverified user can still log in (progressive access policy)")
    void shouldAllowUnverifiedUserToLogin() throws Exception {
        String email = "unverified.login." + System.nanoTime() + "@mediguide.local";
        RegisterRequest registerReq = new RegisterRequest("Login User", email, "Password123!", Role.PATIENT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest(email, "Password123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.user.emailVerified", is(false)));
    }

    @Test
    @DisplayName("3. Valid token verifies email address and enforces single-use behavior")
    void shouldVerifyEmailSuccessfullyAndEnforceSingleUse() throws Exception {
        String email = "verify.singleuse." + System.nanoTime() + "@mediguide.local";
        User user = new User("Single Use User", email, "HashedPassword123", Role.PATIENT, "PAT-889900");
        user.setEmailVerified(false);
        user = userRepository.save(user);

        // Generate token
        String rawToken = emailVerificationService.generateAndSendVerificationToken(user);

        // 1st verify attempt: Success (200 OK)
        VerifyEmailRequest verifyReq = new VerifyEmailRequest(rawToken);
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified", is(true)))
                .andExpect(jsonPath("$.email", is(email)));

        User verifiedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(verifiedUser.isEmailVerified());

        // 2nd verify attempt: Should fail (single-use token consumed)
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid or unrecognized")));
    }

    @Test
    @DisplayName("4. Reject blank or invalid verification token with 400 Bad Request")
    void shouldRejectInvalidOrBlankVerificationToken() throws Exception {
        // Blank token
        VerifyEmailRequest blankReq = new VerifyEmailRequest("   ");
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankReq)))
                .andExpect(status().isBadRequest());

        // Invalid unrecognized token
        VerifyEmailRequest invalidReq = new VerifyEmailRequest("unrecognized-fake-token-value");
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid or unrecognized")));
    }

    @Test
    @DisplayName("5. Resend verification enforces rate limit / cooldown with 429 Too Many Requests")
    void shouldEnforceCooldownOnResendVerification() throws Exception {
        String email = "resend.cooldown." + System.nanoTime() + "@mediguide.local";
        User user = new User("Resend User", email, "HashedPassword123", Role.PATIENT, "PAT-776655");
        user.setEmailVerified(false);
        user = userRepository.save(user);

        // Initial token dispatch
        emailVerificationService.generateAndSendVerificationToken(user);

        // Immediate resend within 60s cooldown -> 429 Too Many Requests
        ResendVerificationRequest resendReq = new ResendVerificationRequest(email);
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.message", containsString("Please wait")));
    }

    @Test
    @DisplayName("6. Resend verification returns uniform anti-enumeration response for non-existent email")
    void shouldReturnUniformResponseForNonExistentEmail() throws Exception {
        ResendVerificationRequest resendReq = new ResendVerificationRequest("ghost." + System.nanoTime() + "@mediguide.local");

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("If an unverified account exists for that email, a verification link has been sent.")));
    }

    @Test
    @DisplayName("7. Unverified patient cannot book consultation appointment (high-trust protection)")
    void shouldBlockUnverifiedPatientFromBookingAppointment() throws Exception {
        // Create unverified patient
        User unverifiedPatient = new User("Unverified Pat", "unverified.book." + System.nanoTime() + "@mediguide.local", "Pass123", Role.PATIENT, "PAT-991122");
        unverifiedPatient.setEmailVerified(false);
        unverifiedPatient = userRepository.save(unverifiedPatient);
        String patientToken = jwtService.generateToken(unverifiedPatient);

        // Create verified doctor with active availability
        User doctor = new User("Dr. Verified", "doctor.v." + System.nanoTime() + "@mediguide.local", "Pass123", Role.DOCTOR, "DOC-991122");
        doctor.setEmailVerified(true);
        doctor = userRepository.save(doctor);

        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);
        DoctorAvailability slot = availabilityRepository.save(new DoctorAvailability(doctor, start, end, true));

        BookAppointmentRequest bookReq = new BookAppointmentRequest(slot.getId(), start, end);

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Email verification is required to book consultation appointments")));
    }

    @Test
    @DisplayName("8. Unverified doctor cannot create availability schedules (high-trust protection)")
    void shouldBlockUnverifiedDoctorFromCreatingAvailability() throws Exception {
        // Create unverified doctor
        User unverifiedDoctor = new User("Dr. Unverified", "unverified.doc." + System.nanoTime() + "@mediguide.local", "Pass123", Role.DOCTOR, "DOC-993344");
        unverifiedDoctor.setEmailVerified(false);
        unverifiedDoctor = userRepository.save(unverifiedDoctor);
        String doctorToken = jwtService.generateToken(unverifiedDoctor);

        LocalDateTime start = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);
        CreateAvailabilityRequest availReq = new CreateAvailabilityRequest(start, end, true);

        mockMvc.perform(post("/api/v1/doctor/availabilities")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(availReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Email verification is required to configure doctor availability schedules")));
    }
}
