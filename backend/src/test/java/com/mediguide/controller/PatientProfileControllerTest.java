package com.mediguide.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediguide.dto.PatientProfileRequest;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import com.mediguide.repository.UserRepository;
import com.mediguide.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatientProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User patient1;
    private User patient2;
    private User doctor;

    private String patient1Token;
    private String patient2Token;
    private String doctorToken;

    @BeforeEach
    void setUp() {
        patient1 = userRepository.save(new User(
                "Aarav Sharma",
                "aarav." + System.nanoTime() + "@mediguide.local",
                passwordEncoder.encode("PatientPass123"),
                Role.PATIENT
        ));
        patient1Token = jwtService.generateToken(patient1);

        patient2 = userRepository.save(new User(
                "Rohan Verma",
                "rohan." + System.nanoTime() + "@mediguide.local",
                passwordEncoder.encode("PatientPass123"),
                Role.PATIENT
        ));
        patient2Token = jwtService.generateToken(patient2);

        doctor = userRepository.save(new User(
                "Dr. Priya Rao",
                "dr.priya." + System.nanoTime() + "@mediguide.local",
                passwordEncoder.encode("DoctorPass123"),
                Role.DOCTOR
        ));
        doctorToken = jwtService.generateToken(doctor);
    }

    @Test
    @DisplayName("Patient can view and update their own phone number")
    void patientCanViewAndUpdateOwnPhoneNumber() throws Exception {
        // 1. Initial retrieval - phone number is null
        mockMvc.perform(get("/api/v1/patient/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId", is(patient1.getPublicId())))
                .andExpect(jsonPath("$.fullName", is("Aarav Sharma")))
                .andExpect(jsonPath("$.phoneNumber", nullValue()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());

        // 2. Update phone number
        PatientProfileRequest request = new PatientProfileRequest("+91 9876543210");
        mockMvc.perform(put("/api/v1/patient/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId", is(patient1.getPublicId())))
                .andExpect(jsonPath("$.phoneNumber", is("+91 9876543210")));

        // 3. Verify updated profile is persisted
        mockMvc.perform(get("/api/v1/patient/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber", is("+91 9876543210")));
    }

    @Test
    @DisplayName("One patient cannot update another patient's profile; updates are strictly isolated to caller principal")
    void patientCannotUpdateAnotherPatientProfile() throws Exception {
        // Patient 1 sets their phone
        PatientProfileRequest p1Req = new PatientProfileRequest("+91 9111111111");
        mockMvc.perform(put("/api/v1/patient/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p1Req)))
                .andExpect(status().isOk());

        // Patient 2 sets their phone
        PatientProfileRequest p2Req = new PatientProfileRequest("+91 9222222222");
        mockMvc.perform(put("/api/v1/patient/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patient2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p2Req)))
                .andExpect(status().isOk());

        // Patient 1's profile remains unchanged
        mockMvc.perform(get("/api/v1/patient/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber", is("+91 9111111111")));
    }

    @Test
    @DisplayName("Doctor cannot access patient profile (returns 403 Forbidden)")
    void doctorCannotAccessPatientProfile() throws Exception {
        mockMvc.perform(get("/api/v1/patient/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + doctorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/patient/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PatientProfileRequest("+91 9876543210"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Invalid phone format is rejected with 400 Bad Request")
    void invalidPhoneFormatIsRejected() throws Exception {
        PatientProfileRequest badReq = new PatientProfileRequest("123");

        mockMvc.perform(put("/api/v1/patient/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/patient/profile"))
                .andExpect(status().isUnauthorized());
    }
}
