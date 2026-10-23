package com.mediguide.service;

import com.mediguide.dto.PatientProfileRequest;
import com.mediguide.dto.PatientProfileResponse;
import com.mediguide.exception.AppointmentValidationException;
import com.mediguide.exception.ResourceNotFoundException;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import com.mediguide.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Service managing patient personal contact details and profile preferences.
 */
@Service
public class PatientProfileService {

    private static final Logger log = LoggerFactory.getLogger(PatientProfileService.class);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-()]{7,20}$");

    private final UserRepository userRepository;

    public PatientProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieves the authenticated patient's profile details.
     *
     * @param patientUserId internal primary key of the patient user
     * @return PatientProfileResponse with safe contact information
     */
    @Transactional(readOnly = true)
    public PatientProfileResponse getPatientProfile(Long patientUserId) {
        if (patientUserId == null) {
            throw new AccessDeniedException("Authentication is required to access patient profile");
        }

        User patient = userRepository.findById(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientUserId));

        if (patient.getRole() != Role.PATIENT) {
            throw new AccessDeniedException("Only patients can access the patient profile");
        }

        return PatientProfileResponse.fromEntity(patient);
    }

    /**
     * Updates the authenticated patient's contact phone number.
     *
     * @param patientUserId internal primary key of the patient user
     * @param request validated contact details
     * @return updated PatientProfileResponse
     */
    @Transactional
    public PatientProfileResponse updatePatientProfile(Long patientUserId, PatientProfileRequest request) {
        if (patientUserId == null) {
            throw new AccessDeniedException("Authentication is required to update patient profile");
        }

        User patient = userRepository.findById(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientUserId));

        if (patient.getRole() != Role.PATIENT) {
            throw new AccessDeniedException("Only patients can update their patient profile");
        }

        String phone = (request != null && request.getPhoneNumber() != null)
                ? request.getPhoneNumber().trim()
                : null;

        if (phone != null && phone.isEmpty()) {
            phone = null;
        }

        if (phone != null && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new AppointmentValidationException("Phone number must be a valid contact number containing 7 to 20 digits");
        }

        patient.setPhoneNumber(phone);
        User saved = userRepository.save(patient);
        log.info("Patient profile updated successfully for publicId: {}", saved.getPublicId());

        return PatientProfileResponse.fromEntity(saved);
    }
}
