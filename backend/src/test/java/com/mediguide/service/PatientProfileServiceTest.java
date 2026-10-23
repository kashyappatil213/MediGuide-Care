package com.mediguide.service;

import com.mediguide.dto.PatientProfileRequest;
import com.mediguide.dto.PatientProfileResponse;
import com.mediguide.exception.AppointmentValidationException;
import com.mediguide.exception.ResourceNotFoundException;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import com.mediguide.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

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
class PatientProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    private PatientProfileService patientProfileService;

    private User patientUser;
    private User doctorUser;

    @BeforeEach
    void setUp() {
        patientProfileService = new PatientProfileService(userRepository);

        patientUser = new User("Rohan Verma", "rohan@mediguide.local", "hashedPass", Role.PATIENT, "PAT-500600");
        patientUser.setId(20L);

        doctorUser = new User("Dr. Priya Rao", "priya.rao@mediguide.local", "hashedPass", Role.DOCTOR, "DOC-100200");
        doctorUser.setId(10L);
    }

    @Test
    @DisplayName("Should successfully retrieve authenticated patient profile")
    void shouldGetPatientProfileSuccessfully() {
        patientUser.setPhoneNumber("+91 9876543210");
        when(userRepository.findById(20L)).thenReturn(Optional.of(patientUser));

        PatientProfileResponse response = patientProfileService.getPatientProfile(20L);

        assertNotNull(response);
        assertEquals("PAT-500600", response.getPublicId());
        assertEquals("Rohan Verma", response.getFullName());
        assertEquals("rohan@mediguide.local", response.getEmail());
        assertEquals("+91 9876543210", response.getPhoneNumber());
    }

    @Test
    @DisplayName("Should reject profile retrieval if user is not a PATIENT")
    void shouldRejectProfileRetrievalForNonPatient() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(doctorUser));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> patientProfileService.getPatientProfile(10L));
        assertTrue(ex.getMessage().contains("Only patients can access"));
    }

    @Test
    @DisplayName("Should successfully update patient phone number")
    void shouldUpdatePatientPhoneNumberSuccessfully() {
        PatientProfileRequest request = new PatientProfileRequest("+91 9123456789");
        when(userRepository.findById(20L)).thenReturn(Optional.of(patientUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        PatientProfileResponse response = patientProfileService.updatePatientProfile(20L, request);

        assertNotNull(response);
        assertEquals("+91 9123456789", response.getPhoneNumber());
        assertEquals("+91 9123456789", patientUser.getPhoneNumber());
        verify(userRepository).save(patientUser);
    }

    @Test
    @DisplayName("Should clear phone number when empty or blank string provided")
    void shouldClearPhoneNumberWhenBlank() {
        patientUser.setPhoneNumber("+91 9876543210");
        PatientProfileRequest request = new PatientProfileRequest("   ");
        when(userRepository.findById(20L)).thenReturn(Optional.of(patientUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        PatientProfileResponse response = patientProfileService.updatePatientProfile(20L, request);

        assertNotNull(response);
        assertNull(response.getPhoneNumber());
        assertNull(patientUser.getPhoneNumber());
    }

    @Test
    @DisplayName("Should reject invalid phone number format")
    void shouldRejectInvalidPhoneNumberFormat() {
        // Too short (< 7 digits)
        PatientProfileRequest reqShort = new PatientProfileRequest("12345");
        when(userRepository.findById(20L)).thenReturn(Optional.of(patientUser));

        assertThrows(AppointmentValidationException.class,
                () -> patientProfileService.updatePatientProfile(20L, reqShort));

        // Invalid characters
        PatientProfileRequest reqLetters = new PatientProfileRequest("98765abcde");
        assertThrows(AppointmentValidationException.class,
                () -> patientProfileService.updatePatientProfile(20L, reqLetters));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject update if user is not found")
    void shouldThrowNotFoundWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> patientProfileService.updatePatientProfile(99L, new PatientProfileRequest("+91 9999999999")));
    }
}
