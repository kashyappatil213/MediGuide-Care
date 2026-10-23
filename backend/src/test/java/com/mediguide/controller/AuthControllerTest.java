package com.mediguide.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediguide.dto.LoginRequest;
import com.mediguide.dto.RegisterRequest;
import com.mediguide.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("1. Valid patient registration returns 201 Created, safe UserResponse, and PAT- publicId")
    void shouldRegisterValidPatientSuccessfully() throws Exception {
        String email = "patient." + System.nanoTime() + "@mediguide.local";
        RegisterRequest request = new RegisterRequest("Sunita Verma", email, "SecurePassword123", Role.PATIENT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.publicId", org.hamcrest.Matchers.startsWith("PAT-")))
                .andExpect(jsonPath("$.fullName", is("Sunita Verma")))
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.role", is("PATIENT")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("2. Valid doctor registration returns 201 Created and DOC- publicId")
    void shouldRegisterValidDoctorSuccessfully() throws Exception {
        String email = "doctor." + System.nanoTime() + "@mediguide.local";
        RegisterRequest request = new RegisterRequest("Dr. Sameer Sen", email, "DoctorSecretPass1", Role.DOCTOR);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.publicId", org.hamcrest.Matchers.startsWith("DOC-")))
                .andExpect(jsonPath("$.fullName", is("Dr. Sameer Sen")))
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.role", is("DOCTOR")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("3. Missing required fields or invalid email returns 400 Bad Request with field errors")
    void shouldRejectMissingFieldsAndInvalidEmail() throws Exception {
        // Missing name, invalid email
        String jsonPayload = """
                {
                    "fullName": "",
                    "email": "not-a-valid-email",
                    "password": "ValidPassword123",
                    "role": "PATIENT"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.fieldErrors.fullName", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.email", notNullValue()));
    }

    @Test
    @DisplayName("4. Password shorter than 8 characters returns 400 Bad Request")
    void shouldRejectShortPassword() throws Exception {
        String email = "shortpass." + System.nanoTime() + "@mediguide.local";
        RegisterRequest request = new RegisterRequest("Test User", email, "pass7", Role.PATIENT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.fieldErrors.password", containsString("8 characters")));
    }

    @Test
    @DisplayName("5. ADMIN registration attempt returns 400 Bad Request")
    void shouldRejectAdminRegistrationAttempt() throws Exception {
        String email = "admin." + System.nanoTime() + "@mediguide.local";
        RegisterRequest request = new RegisterRequest("Admin Attempt", email, "SuperSecurePass123", Role.ADMIN);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("ADMIN")));
    }

    @Test
    @DisplayName("6. Duplicate normalized email returns 409 Conflict")
    void shouldRejectDuplicateNormalizedEmail() throws Exception {
        String baseEmail = "unique.dup." + System.nanoTime() + "@mediguide.local";
        RegisterRequest first = new RegisterRequest("First User", baseEmail, "PasswordFirst123", Role.PATIENT);

        // First registration succeeds
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        // Second registration with spaces and mixed casing
        String mixedEmail = "  " + baseEmail.toUpperCase() + "  ";
        RegisterRequest duplicate = new RegisterRequest("Second User", mixedEmail, "PasswordSecond123", Role.DOCTOR);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("7. Malformed JSON or invalid role returns 400 Bad Request")
    void shouldRejectMalformedJsonOrInvalidRole() throws Exception {
        String malformedJson = """
                {
                    "fullName": "Test User",
                    "email": "test@mediguide.local",
                    "password": "Password123",
                    "role": "SUPERUSER_INVALID"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Malformed JSON")));
    }

    @Test
    @DisplayName("8. Failed error responses do not expose credentials, stack traces, or sensitive internals")
    void errorResponseMustNotExposeSensitiveDetails() throws Exception {
        String malformedJson = "{ invalid json body ";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // --- Login Endpoint Tests ---

    @Test
    @DisplayName("9. Successful login returns 200 OK with signed JWT and safe user details")
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        String email = "login.success." + System.nanoTime() + "@mediguide.local";
        String rawPassword = "StrongPassword123";

        // Register user first
        RegisterRequest registerRequest = new RegisterRequest("Kavita Nair", email, rawPassword, Role.PATIENT);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Perform login
        LoginRequest loginRequest = new LoginRequest(email, rawPassword);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(900)))
                .andExpect(jsonPath("$.user.id", notNullValue()))
                .andExpect(jsonPath("$.user.fullName", is("Kavita Nair")))
                .andExpect(jsonPath("$.user.email", is(email)))
                .andExpect(jsonPath("$.user.role", is("PATIENT")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("10. Unknown email returns 401 Unauthorized with generic message")
    void shouldReturn401ForUnknownEmail() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent.user@mediguide.local", "AnyPassword123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("11. Incorrect password returns 401 Unauthorized with same generic message")
    void shouldReturn401ForIncorrectPassword() throws Exception {
        String email = "login.wrongpass." + System.nanoTime() + "@mediguide.local";
        RegisterRequest registerRequest = new RegisterRequest("Vikram Seth", email, "RealPassword123", Role.DOCTOR);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(email, "WrongPassword999");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("12. Login normalizes email with whitespace and uppercase")
    void shouldLoginWithNormalizedEmail() throws Exception {
        String baseEmail = "login.norm." + System.nanoTime() + "@mediguide.local";
        String rawPassword = "CorrectPass123";

        RegisterRequest registerRequest = new RegisterRequest("Norm User", baseEmail, rawPassword, Role.PATIENT);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        String mixedEmail = "   " + baseEmail.toUpperCase() + "   ";
        LoginRequest loginRequest = new LoginRequest(mixedEmail, rawPassword);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.user.email", is(baseEmail)));
    }

    @Test
    @DisplayName("13. Blank login fields return 400 Bad Request")
    void shouldReturn400ForBlankLoginFields() throws Exception {
        LoginRequest loginRequest = new LoginRequest("", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.fieldErrors.email", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.password", notNullValue()));
    }

    @Test
    @DisplayName("14. Two newly registered users receive distinct public IDs")
    void twoRegistrations_receiveDistinctPublicIds() throws Exception {
        String email1 = "pat1." + System.nanoTime() + "@mediguide.local";
        String email2 = "pat2." + System.nanoTime() + "@mediguide.local";

        RegisterRequest req1 = new RegisterRequest("User One", email1, "SecurePass123", Role.PATIENT);
        RegisterRequest req2 = new RegisterRequest("User Two", email2, "SecurePass123", Role.PATIENT);

        String res1 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String res2 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id1 = objectMapper.readTree(res1).get("publicId").asText();
        String id2 = objectMapper.readTree(res2).get("publicId").asText();

        org.junit.jupiter.api.Assertions.assertNotNull(id1);
        org.junit.jupiter.api.Assertions.assertNotNull(id2);
        org.junit.jupiter.api.Assertions.assertTrue(id1.startsWith("PAT-"));
        org.junit.jupiter.api.Assertions.assertTrue(id2.startsWith("PAT-"));
        org.junit.jupiter.api.Assertions.assertNotEquals(id1, id2, "Public IDs for distinct users must be unique");
    }

    @Test
    @DisplayName("15. User public ID is persisted and returned consistently on login")
    void publicId_persistedAndReturnedConsistentlyOnLogin() throws Exception {
        String email = "doctor.login.id." + System.nanoTime() + "@mediguide.local";
        String password = "DoctorPassword123";

        RegisterRequest regReq = new RegisterRequest("Dr. Meera Nambiar", email, password, Role.DOCTOR);
        String regResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId", org.hamcrest.Matchers.startsWith("DOC-")))
                .andReturn().getResponse().getContentAsString();

        String expectedPublicId = objectMapper.readTree(regResponse).get("publicId").asText();

        LoginRequest loginReq = new LoginRequest(email, password);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.publicId", is(expectedPublicId)))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("16. Client cannot supply or override generated public ID")
    void clientCannotSupplyOrOverridePublicId() throws Exception {
        String email = "override." + System.nanoTime() + "@mediguide.local";
        String payloadWithForgedId = """
                {
                    "fullName": "Imposter User",
                    "email": "%s",
                    "password": "SecurePassword123",
                    "role": "PATIENT",
                    "publicId": "DOC-FORGED99"
                }
                """.formatted(email);

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithForgedId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String assignedPublicId = objectMapper.readTree(response).get("publicId").asText();
        org.junit.jupiter.api.Assertions.assertTrue(assignedPublicId.startsWith("PAT-"),
                "Generated ID must have PAT- prefix for patient role");
        org.junit.jupiter.api.Assertions.assertNotEquals("DOC-FORGED99", assignedPublicId,
                "Client-supplied publicId must be ignored");
    }
}
