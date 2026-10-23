package com.mediguide.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediguide.dto.LoginRequest;
import com.mediguide.dto.RegisterRequest;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import com.mediguide.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    private User patientUser;
    private User doctorUser;
    private String patientToken;
    private String doctorToken;

    @BeforeEach
    void setUp() {
        patientUser = new User("Anjali Gupta", "anjali." + System.nanoTime() + "@mediguide.local", "hash123", Role.PATIENT);
        patientUser.setId(201L);
        patientToken = jwtService.generateToken(patientUser);

        doctorUser = new User("Dr. Ramesh Sen", "doctor." + System.nanoTime() + "@mediguide.local", "hash123", Role.DOCTOR);
        doctorUser.setId(202L);
        doctorToken = jwtService.generateToken(doctorUser);
    }

    @Test
    @DisplayName("1. Registration endpoint remains public without authentication header")
    void registrationEndpointRemainsPublic() throws Exception {
        String email = "public.reg." + System.nanoTime() + "@mediguide.local";
        RegisterRequest request = new RegisterRequest("New Patient", email, "SecurePass123", Role.PATIENT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.role", is("PATIENT")));
    }

    @Test
    @DisplayName("2. Login endpoint remains public without authentication header")
    void loginEndpointRemainsPublic() throws Exception {
        // Register a user first
        String email = "login.test." + System.nanoTime() + "@mediguide.local";
        RegisterRequest registerReq = new RegisterRequest("Login User", email, "LoginPass123", Role.PATIENT);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Perform login without any Authorization header
        LoginRequest loginReq = new LoginRequest(email, "LoginPass123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")));
    }

    @Test
    @DisplayName("3. Health endpoints remain public without authentication")
    void healthEndpointsRemainPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("4. Protected test endpoint returns 401 Unauthorized when no token is supplied")
    void protectedEndpointReturns401WhenNoTokenSupplied() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("authentication is required")));
    }

    @Test
    @DisplayName("5a. Protected test endpoint returns 401 for a malformed token")
    void protectedEndpointReturns401ForMalformedToken() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer malformed.invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("authentication is required")));
    }

    @Test
    @DisplayName("5b. Protected test endpoint returns 401 for a tampered token")
    void protectedEndpointReturns401ForTamperedToken() throws Exception {
        String tamperedToken = patientToken.substring(0, patientToken.length() - 8) + "xxxxxxxx";

        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("5c. Protected test endpoint returns 401 for an expired token")
    void protectedEndpointReturns401ForExpiredToken() throws Exception {
        Instant pastIssuedAt = Instant.now().minusSeconds(7200);
        Instant pastExpiresAt = Instant.now().minusSeconds(3600);
        String expiredToken = jwtService.generateToken(patientUser, pastIssuedAt, pastExpiresAt);

        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("5d. Protected test endpoint returns 401 when non-Bearer scheme or empty token is passed")
    void protectedEndpointReturns401ForInvalidHeaderScheme() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("6. Protected test endpoint succeeds with a valid JWT")
    void protectedEndpointSucceedsWithValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("authenticated")))
                .andExpect(jsonPath("$.message", is("Access granted to protected test endpoint")));
    }

    @Test
    @DisplayName("7. Authenticated principal contains the expected user identity and role authority")
    void authenticatedPrincipalContainsExpectedIdentityAndRole() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(201)))
                .andExpect(jsonPath("$.email", is(patientUser.getEmail())))
                .andExpect(jsonPath("$.role", is("PATIENT")));
    }

    @Test
    @DisplayName("8a. Role-restricted test route returns 403 Forbidden for valid user with wrong role")
    void doctorOnlyRouteReturns403ForPatientUser() throws Exception {
        mockMvc.perform(get("/api/v1/test/doctor-only")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("8b. Role-restricted test route succeeds for allowed DOCTOR role")
    void doctorOnlyRouteSucceedsForDoctorUser() throws Exception {
        mockMvc.perform(get("/api/v1/test/doctor-only")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("authenticated")))
                .andExpect(jsonPath("$.userId", is(202)))
                .andExpect(jsonPath("$.email", is(doctorUser.getEmail())))
                .andExpect(jsonPath("$.role", is("DOCTOR")));
    }

    @Test
    @DisplayName("9. Error responses do not expose tokens, secrets, stack traces, or internal exception details")
    void errorResponsesDoNotExposeSensitiveDetails() throws Exception {
        // Test 401 response structure
        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer invalid-signature-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());

        // Test 403 response structure
        mockMvc.perform(get("/api/v1/test/doctor-only")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }
}
