package com.mediguide.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediguide.dto.DoctorProfileRequest;
import com.mediguide.dto.LoginRequest;
import com.mediguide.model.DoctorProfile;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import com.mediguide.repository.DoctorProfileRepository;
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

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DoctorProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User doctor1;
    private User doctor2;
    private User patient;

    private String doctor1Token;
    private String doctor2Token;
    private String patientToken;

    @BeforeEach
    void setUp() {
        doctor1 = userRepository.save(new User(
                "Dr. Aarav Patel",
                "dr.aarav." + System.nanoTime() + "@mediguide.local",
                passwordEncoder.encode("DoctorPass123"),
                Role.DOCTOR
        ));
        doctor1Token = jwtService.generateToken(doctor1);

        doctor2 = userRepository.save(new User(
                "Dr. Meera Nair",
                "dr.meera." + System.nanoTime() + "@mediguide.local",
                passwordEncoder.encode("DoctorPass123"),
                Role.DOCTOR
        ));
        doctor2Token = jwtService.generateToken(doctor2);

        patient = userRepository.save(new User(
                "Vikram Seth",
                "vikram." + System.nanoTime() + "@mediguide.local",
                passwordEncoder.encode("PatientPass123"),
                Role.PATIENT
        ));
        patientToken = jwtService.generateToken(patient);
    }

    @Test
    @DisplayName("Doctor can create and update their own profile via PUT /api/v1/doctor/profile")
    void doctorCanCreateAndUpdateOwnProfile() throws Exception {
        DoctorProfileRequest createRequest = new DoctorProfileRequest(
                "Cardiology", "Apex Heart Hospital", "12 Ring Road", "Bengaluru", "Indiranagar", "560038"
        );

        // 1. Initial creation
        mockMvc.perform(put("/api/v1/doctor/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.publicId", is(doctor1.getPublicId())))
                .andExpect(jsonPath("$.fullName", is("Dr. Aarav Patel")))
                .andExpect(jsonPath("$.specialty", is("Cardiology")))
                .andExpect(jsonPath("$.clinicName", is("Apex Heart Hospital")))
                .andExpect(jsonPath("$.city", is("Bengaluru")))
                .andExpect(jsonPath("$.pinCode", is("560038")));

        // 2. Fetch own profile
        mockMvc.perform(get("/api/v1/doctor/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + doctor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialty", is("Cardiology")))
                .andExpect(jsonPath("$.clinicName", is("Apex Heart Hospital")));

        // 3. Update existing profile
        DoctorProfileRequest updateRequest = new DoctorProfileRequest(
                "Interventional Cardiology", "Fortis Heart Institute", "99 Bannerghatta Road", "Bengaluru", "Jayanagar", "560076"
        );

        mockMvc.perform(put("/api/v1/doctor/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialty", is("Interventional Cardiology")))
                .andExpect(jsonPath("$.clinicName", is("Fortis Heart Institute")))
                .andExpect(jsonPath("$.pinCode", is("560076")));
    }

    @Test
    @DisplayName("Patient cannot create or edit doctor profile (returns 403 Forbidden)")
    void patientCannotCreateOrEditDoctorProfile() throws Exception {
        DoctorProfileRequest request = new DoctorProfileRequest(
                "Cardiology", "Fake Clinic", null, "Mumbai", null, "400001"
        );

        mockMvc.perform(put("/api/v1/doctor/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("One doctor cannot edit another doctor's profile; updates are strictly isolated to caller principal")
    void doctorCannotEditAnotherDoctorProfile() throws Exception {
        // Doctor 1 creates their profile
        DoctorProfileRequest d1Req = new DoctorProfileRequest(
                "Neurology", "Brain Care Center", null, "Delhi", null, "110001"
        );
        mockMvc.perform(put("/api/v1/doctor/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(d1Req)))
                .andExpect(status().isOk());

        // Doctor 2 creates their profile
        DoctorProfileRequest d2Req = new DoctorProfileRequest(
                "Pediatrics", "Kids Health Hub", null, "Delhi", null, "110002"
        );
        mockMvc.perform(put("/api/v1/doctor/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + doctor2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(d2Req)))
                .andExpect(status().isOk());

        // Verify Doctor 1 profile remains unaffected
        mockMvc.perform(get("/api/v1/doctor/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + doctor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialty", is("Neurology")))
                .andExpect(jsonPath("$.clinicName", is("Brain Care Center")));
    }

    @Test
    @DisplayName("Unconfigured doctor profile returns 404 Not Found")
    void unconfiguredDoctorProfileReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/doctor/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + doctor2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Existing doctor without a profile can still log in normally")
    void existingDoctorWithoutProfileCanStillLogin() throws Exception {
        LoginRequest loginRequest = new LoginRequest(doctor2.getEmail(), "DoctorPass123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.user.id", is(doctor2.getId().intValue())))
                .andExpect(jsonPath("$.user.role", is("DOCTOR")));
    }

    @Test
    @DisplayName("Patient can retrieve public doctor profile by public ID without exposing sensitive fields")
    void patientCanRetrievePublicDoctorProfileByPublicId() throws Exception {
        doctorProfileRepository.save(new DoctorProfile(
                doctor1, "Orthopedics", "Bone and Joint Clinic", "55 MG Road", "Chennai", "Adyar", "600020"
        ));

        mockMvc.perform(get("/api/v1/doctors/" + doctor1.getPublicId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId", is(doctor1.getPublicId())))
                .andExpect(jsonPath("$.doctorName", is(doctor1.getFullName())))
                .andExpect(jsonPath("$.specialty", is("Orthopedics")))
                .andExpect(jsonPath("$.clinicName", is("Bone and Joint Clinic")))
                .andExpect(jsonPath("$.city", is("Chennai")))
                .andExpect(jsonPath("$.area", is("Adyar")))
                .andExpect(jsonPath("$.pinCode", is("600020")))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    @DisplayName("Search filters work independently and in combination with case-insensitivity and AND semantics")
    void searchFiltersWorkIndependentlyAndCombined() throws Exception {
        doctorProfileRepository.save(new DoctorProfile(
                doctor1, "Cardiology", "Apollo Heart", "Greams Road", "Chennai", "Thousand Lights", "600006"
        ));
        doctorProfileRepository.save(new DoctorProfile(
                doctor2, "Dermatology", "Glow Skin Care", "Bandra Link", "Mumbai", "Bandra West", "400050"
        ));

        // 1. Specialty filter (case-insensitive substring)
        mockMvc.perform(get("/api/v1/doctors/search?specialty=cardio")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].specialty", is("Cardiology")))
                .andExpect(jsonPath("$[0].doctorName", is(doctor1.getFullName())));

        // 2. City filter (case-insensitive)
        mockMvc.perform(get("/api/v1/doctors/search?city=mumbai")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].specialty", is("Dermatology")));

        // 3. Area filter (case-insensitive substring)
        mockMvc.perform(get("/api/v1/doctors/search?area=bandra")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].area", is("Bandra West")));

        // 4. Exact PIN code match
        mockMvc.perform(get("/api/v1/doctors/search?pinCode=600006")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].publicId", is(doctor1.getPublicId())));

        // 5. Combined match (AND semantics)
        mockMvc.perform(get("/api/v1/doctors/search?specialty=derma&city=mumbai")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].doctorName", is(doctor2.getFullName())));

        // 6. Mismatched combined filters return empty list
        mockMvc.perform(get("/api/v1/doctors/search?specialty=cardio&city=mumbai")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Search response never exposes email, password, or numeric database IDs")
    void searchResponseNeverExposesSensitiveData() throws Exception {
        doctorProfileRepository.save(new DoctorProfile(
                doctor1, "General Medicine", "City Health", null, "Pune", null, "411001"
        ));

        mockMvc.perform(get("/api/v1/doctors/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[*].email").doesNotExist())
                .andExpect(jsonPath("$[*].password").doesNotExist())
                .andExpect(jsonPath("$[*].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[*].id").doesNotExist());
    }

    @Test
    @DisplayName("Invalid Indian PIN code is rejected with 400 Bad Request")
    void invalidPinCodeIsRejected() throws Exception {
        DoctorProfileRequest badPinReq = new DoctorProfileRequest(
                "Cardiology", "Heart Center", null, "Delhi", null, "123"
        );

        mockMvc.perform(put("/api/v1/doctor/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badPinReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Doctor can update clinic and personal phone; public endpoints return clinicPhone and never personalPhone")
    void doctorCanUpdatePhoneNumbersAndPublicOmitsPersonalPhone() throws Exception {
        DoctorProfileRequest request = new DoctorProfileRequest(
                "Cardiology", "City Heart Clinic", "10 Residency Road", "Bengaluru", "CBD", "560025",
                "+91 80 44556677", "+91 9888877777"
        );

        // 1. Doctor updates profile with both numbers
        mockMvc.perform(put("/api/v1/doctor/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clinicPhone", is("+91 80 44556677")))
                .andExpect(jsonPath("$.personalPhone", is("+91 9888877777")));

        // 2. Doctor retrieves own profile - both numbers present
        mockMvc.perform(get("/api/v1/doctor/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + doctor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clinicPhone", is("+91 80 44556677")))
                .andExpect(jsonPath("$.personalPhone", is("+91 9888877777")));

        // 3. Public profile lookup - includes clinicPhone, strictly omits personalPhone
        mockMvc.perform(get("/api/v1/doctors/" + doctor1.getPublicId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clinicPhone", is("+91 80 44556677")))
                .andExpect(jsonPath("$.personalPhone").doesNotExist());

        // 4. Public search - includes clinicPhone, strictly omits personalPhone
        mockMvc.perform(get("/api/v1/doctors/search?specialty=cardio")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].clinicPhone", is("+91 80 44556677")))
                .andExpect(jsonPath("$[0].personalPhone").doesNotExist());
    }
}
