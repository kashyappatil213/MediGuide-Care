package com.mediguide.service;

import com.mediguide.dto.DoctorProfileRequest;
import com.mediguide.dto.DoctorProfileResponse;
import com.mediguide.dto.PublicDoctorProfileResponse;
import com.mediguide.exception.AppointmentValidationException;
import com.mediguide.exception.ResourceNotFoundException;
import com.mediguide.model.DoctorProfile;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import com.mediguide.repository.DoctorProfileRepository;
import com.mediguide.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorProfileServiceTest {

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private UserRepository userRepository;

    private DoctorProfileService doctorProfileService;

    private User doctorUser;
    private User patientUser;

    @BeforeEach
    void setUp() {
        doctorProfileService = new DoctorProfileService(doctorProfileRepository, userRepository);

        doctorUser = new User("Dr. Priya Rao", "priya.rao@mediguide.local", "hashedPass", Role.DOCTOR, "DOC-100200");
        doctorUser.setId(10L);

        patientUser = new User("Rohan Verma", "rohan@mediguide.local", "hashedPass", Role.PATIENT, "PAT-500600");
        patientUser.setId(20L);
    }

    @Test
    @DisplayName("Should successfully create a new doctor profile")
    void shouldCreateDoctorProfileSuccessfully() {
        DoctorProfileRequest request = new DoctorProfileRequest(
                "Cardiology", "HeartCare Clinic", "123 MG Road", "Bengaluru", "Indiranagar", "560038"
        );

        when(userRepository.findById(10L)).thenReturn(Optional.of(doctorUser));
        when(doctorProfileRepository.findByDoctorId(10L)).thenReturn(Optional.empty());
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> {
            DoctorProfile p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        DoctorProfileResponse response = doctorProfileService.createOrUpdateProfile(10L, request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("DOC-100200", response.getPublicId());
        assertEquals("Dr. Priya Rao", response.getFullName());
        assertEquals("Cardiology", response.getSpecialty());
        assertEquals("HeartCare Clinic", response.getClinicName());
        assertEquals("123 MG Road", response.getClinicAddress());
        assertEquals("Bengaluru", response.getCity());
        assertEquals("Indiranagar", response.getArea());
        assertEquals("560038", response.getPinCode());

        verify(doctorProfileRepository).save(any(DoctorProfile.class));
    }

    @Test
    @DisplayName("Should successfully update an existing doctor profile")
    void shouldUpdateExistingDoctorProfileSuccessfully() {
        DoctorProfile existing = new DoctorProfile(
                doctorUser, "General Medicine", "Old Clinic", null, "Mumbai", null, null
        );
        existing.setId(5L);

        DoctorProfileRequest updateRequest = new DoctorProfileRequest(
                "Cardiology", "New Advanced Heart Center", "45 Marine Drive", "Mumbai", "Nariman Point", "400021"
        );

        when(userRepository.findById(10L)).thenReturn(Optional.of(doctorUser));
        when(doctorProfileRepository.findByDoctorId(10L)).thenReturn(Optional.of(existing));
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorProfileResponse response = doctorProfileService.createOrUpdateProfile(10L, updateRequest);

        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals("Cardiology", response.getSpecialty());
        assertEquals("New Advanced Heart Center", response.getClinicName());
        assertEquals("400021", response.getPinCode());
    }

    @Test
    @DisplayName("Should reject profile creation if user is not a DOCTOR")
    void shouldRejectProfileCreationForNonDoctor() {
        DoctorProfileRequest request = new DoctorProfileRequest(
                "Cardiology", "HeartCare Clinic", "123 MG Road", "Bengaluru", "Indiranagar", "560038"
        );

        when(userRepository.findById(20L)).thenReturn(Optional.of(patientUser));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> doctorProfileService.createOrUpdateProfile(20L, request));

        assertTrue(ex.getMessage().contains("Only doctors can create or update"));
        verify(doctorProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject invalid Indian PIN code format")
    void shouldRejectInvalidPinCodeFormat() {
        // Less than 6 digits
        DoctorProfileRequest reqShort = new DoctorProfileRequest("Cardiology", "Clinic", null, "City", null, "12345");
        when(userRepository.findById(10L)).thenReturn(Optional.of(doctorUser));
        assertThrows(AppointmentValidationException.class, () -> doctorProfileService.createOrUpdateProfile(10L, reqShort));

        // Starts with 0
        DoctorProfileRequest reqZeroStart = new DoctorProfileRequest("Cardiology", "Clinic", null, "City", null, "012345");
        assertThrows(AppointmentValidationException.class, () -> doctorProfileService.createOrUpdateProfile(10L, reqZeroStart));

        // Alphabetic characters
        DoctorProfileRequest reqAlpha = new DoctorProfileRequest("Cardiology", "Clinic", null, "City", null, "56000A");
        assertThrows(AppointmentValidationException.class, () -> doctorProfileService.createOrUpdateProfile(10L, reqAlpha));
    }

    @Test
    @DisplayName("Should allow null or blank PIN code as an optional field")
    void shouldAllowNullOrBlankPinCode() {
        DoctorProfileRequest request = new DoctorProfileRequest(
                "Dermatology", "Skin Care Hub", null, "Delhi", null, "   "
        );

        when(userRepository.findById(10L)).thenReturn(Optional.of(doctorUser));
        when(doctorProfileRepository.findByDoctorId(10L)).thenReturn(Optional.empty());
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorProfileResponse response = doctorProfileService.createOrUpdateProfile(10L, request);

        assertNotNull(response);
        assertNull(response.getPinCode());
    }

    @Test
    @DisplayName("Should retrieve current doctor's profile")
    void shouldGetMyProfileSuccessfully() {
        DoctorProfile existing = new DoctorProfile(
                doctorUser, "Pediatrics", "Children Care", "78 Ring Road", "Pune", "Kothrud", "411038"
        );
        existing.setId(7L);

        when(doctorProfileRepository.findByDoctorId(10L)).thenReturn(Optional.of(existing));

        DoctorProfileResponse response = doctorProfileService.getMyProfile(10L);

        assertNotNull(response);
        assertEquals(7L, response.getId());
        assertEquals("DOC-100200", response.getPublicId());
        assertEquals("Pediatrics", response.getSpecialty());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when doctor profile does not exist yet")
    void shouldThrowWhenMyProfileNotFound() {
        when(doctorProfileRepository.findByDoctorId(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> doctorProfileService.getMyProfile(10L));
    }

    @Test
    @DisplayName("Should retrieve public profile by public ID without exposing sensitive fields")
    void shouldGetPublicProfileByPublicId() {
        DoctorProfile profile = new DoctorProfile(
                doctorUser, "Neurology", "NeuroCare", "10 Science City", "Kolkata", "Salt Lake", "700091"
        );
        profile.setId(8L);

        when(doctorProfileRepository.findByDoctorPublicId("DOC-100200")).thenReturn(Optional.of(profile));

        PublicDoctorProfileResponse response = doctorProfileService.getPublicProfileByPublicId("DOC-100200");

        assertNotNull(response);
        assertEquals("DOC-100200", response.getPublicId());
        assertEquals("Dr. Priya Rao", response.getDoctorName());
        assertEquals("Neurology", response.getSpecialty());
        assertEquals("NeuroCare", response.getClinicName());
        assertEquals("Kolkata", response.getCity());
        assertEquals("700091", response.getPinCode());
    }

    @Test
    @DisplayName("Search should sanitize page size to prevent unbounded results")
    void shouldSanitizePageSizeToPreventUnboundedResults() {
        DoctorProfile profile = new DoctorProfile(
                doctorUser, "Cardiology", "HeartCare Clinic", null, "Bengaluru", null, "560038"
        );
        Page<DoctorProfile> pagedResult = new PageImpl<>(List.of(profile));

        when(doctorProfileRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagedResult);

        // Requested size 500 should be capped at MAX_PAGE_SIZE (50)
        List<PublicDoctorProfileResponse> results = doctorProfileService.searchPublicProfiles(
                "Cardio", "Bengaluru", null, null, 0, 500
        );

        assertNotNull(results);
        assertEquals(1, results.size());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(doctorProfileRepository).findAll(any(Specification.class), captor.capture());
        Pageable usedPageable = captor.getValue();
        assertEquals(DoctorProfileService.MAX_PAGE_SIZE, usedPageable.getPageSize());
    }

    @Test
    @DisplayName("Should save and retrieve clinic and personal phone numbers for authenticated doctor")
    void shouldSaveAndRetrieveClinicAndPersonalPhoneNumbers() {
        DoctorProfileRequest request = new DoctorProfileRequest(
                "Cardiology", "HeartCare Clinic", "123 MG Road", "Bengaluru", "Indiranagar", "560038",
                "+91 80 12345678", "+91 9876543210"
        );

        when(userRepository.findById(10L)).thenReturn(Optional.of(doctorUser));
        when(doctorProfileRepository.findByDoctorId(10L)).thenReturn(Optional.empty());
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorProfileResponse response = doctorProfileService.createOrUpdateProfile(10L, request);

        assertNotNull(response);
        assertEquals("+91 80 12345678", response.getClinicPhone());
        assertEquals("+91 9876543210", response.getPersonalPhone());
    }

    @Test
    @DisplayName("Public profile should include clinic phone but strictly omit personal phone")
    void shouldIncludeClinicPhoneAndOmitPersonalPhoneInPublicProfile() {
        DoctorProfile profile = new DoctorProfile(
                doctorUser, "Neurology", "NeuroCare", "10 Science City", "Kolkata", "Salt Lake", "700091",
                "+91 33 22223333", "+91 9999988888"
        );
        when(doctorProfileRepository.findByDoctorPublicId("DOC-100200")).thenReturn(Optional.of(profile));

        PublicDoctorProfileResponse response = doctorProfileService.getPublicProfileByPublicId("DOC-100200");

        assertNotNull(response);
        assertEquals("+91 33 22223333", response.getClinicPhone());
        // Verify personal phone is never returned in public DTO
        for (java.lang.reflect.Field field : PublicDoctorProfileResponse.class.getDeclaredFields()) {
            org.junit.jupiter.api.Assertions.assertNotEquals("personalPhone", field.getName(),
                    "PublicDoctorProfileResponse must never expose doctor's personal phone field");
        }
    }

    @Test
    @DisplayName("Should reject invalid phone format for clinic or personal phone")
    void shouldRejectInvalidPhoneFormats() {
        // Bad clinic phone
        DoctorProfileRequest reqBadClinic = new DoctorProfileRequest(
                "Cardiology", "Clinic", null, "City", null, null, "123", "+91 9876543210"
        );
        when(userRepository.findById(10L)).thenReturn(Optional.of(doctorUser));
        assertThrows(AppointmentValidationException.class, () -> doctorProfileService.createOrUpdateProfile(10L, reqBadClinic));

        // Bad personal phone
        DoctorProfileRequest reqBadPersonal = new DoctorProfileRequest(
                "Cardiology", "Clinic", null, "City", null, null, "+91 80 12345678", "bad-phone-number"
        );
        assertThrows(AppointmentValidationException.class, () -> doctorProfileService.createOrUpdateProfile(10L, reqBadPersonal));
    }
}
