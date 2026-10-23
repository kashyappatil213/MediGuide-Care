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
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service managing doctor clinical profiles and patient-facing doctor discovery search.
 */
@Service
public class DoctorProfileService {

    private static final Logger log = LoggerFactory.getLogger(DoctorProfileService.class);
    private static final Pattern PIN_CODE_PATTERN = Pattern.compile("^[1-9][0-9]{5}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-()]{7,20}$");
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;

    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;

    public DoctorProfileService(DoctorProfileRepository doctorProfileRepository, UserRepository userRepository) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.userRepository = userRepository;
    }

    /**
     * Retrieves the profile of the authenticated doctor.
     *
     * @param doctorUserId internal primary key of the doctor user
     * @return DoctorProfileResponse
     * @throws ResourceNotFoundException if user or profile is not found
     */
    @Transactional(readOnly = true)
    public DoctorProfileResponse getMyProfile(Long doctorUserId) {
        if (doctorUserId == null) {
            throw new AccessDeniedException("Authentication required to access doctor profile");
        }

        DoctorProfile profile = doctorProfileRepository.findByDoctorId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile has not been configured yet"));

        return DoctorProfileResponse.fromEntity(profile);
    }

    /**
     * Creates or updates the authenticated doctor's profile.
     * Uses the authenticated user ID exclusively to prevent tampering with other profiles.
     *
     * @param doctorUserId internal primary key of the doctor user
     * @param request validated profile details
     * @return updated DoctorProfileResponse
     */
    @Transactional
    public DoctorProfileResponse createOrUpdateProfile(Long doctorUserId, DoctorProfileRequest request) {
        if (doctorUserId == null) {
            throw new AccessDeniedException("Authentication required to configure doctor profile");
        }

        User doctor = userRepository.findById(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found with id: " + doctorUserId));

        if (doctor.getRole() != Role.DOCTOR) {
            throw new AccessDeniedException("Only doctors can create or update a doctor profile");
        }

        String specialty = request.getSpecialty() != null ? request.getSpecialty().trim() : "";
        String clinicName = request.getClinicName() != null ? request.getClinicName().trim() : "";
        String clinicAddress = request.getClinicAddress() != null && !request.getClinicAddress().trim().isEmpty()
                ? request.getClinicAddress().trim() : null;
        String city = request.getCity() != null ? request.getCity().trim() : "";
        String area = request.getArea() != null && !request.getArea().trim().isEmpty()
                ? request.getArea().trim() : null;
        String pinCode = request.getPinCode() != null && !request.getPinCode().trim().isEmpty()
                ? request.getPinCode().trim() : null;
        String clinicPhone = request.getClinicPhone() != null && !request.getClinicPhone().trim().isEmpty()
                ? request.getClinicPhone().trim() : null;
        String personalPhone = request.getPersonalPhone() != null && !request.getPersonalPhone().trim().isEmpty()
                ? request.getPersonalPhone().trim() : null;

        if (specialty.isEmpty()) {
            throw new AppointmentValidationException("Specialty is required");
        }
        if (clinicName.isEmpty()) {
            throw new AppointmentValidationException("Clinic name is required");
        }
        if (city.isEmpty()) {
            throw new AppointmentValidationException("City is required");
        }
        if (pinCode != null && !PIN_CODE_PATTERN.matcher(pinCode).matches()) {
            throw new AppointmentValidationException("PIN code must be a valid 6-digit Indian postal code (e.g. 560001)");
        }
        if (clinicPhone != null && !PHONE_PATTERN.matcher(clinicPhone).matches()) {
            throw new AppointmentValidationException("Clinic contact phone number must be a valid contact number (7-20 digits)");
        }
        if (personalPhone != null && !PHONE_PATTERN.matcher(personalPhone).matches()) {
            throw new AppointmentValidationException("Personal phone number must be a valid contact number (7-20 digits)");
        }

        DoctorProfile profile = doctorProfileRepository.findByDoctorId(doctorUserId)
                .orElseGet(() -> new DoctorProfile(doctor, specialty, clinicName, clinicAddress, city, area, pinCode, clinicPhone, personalPhone));

        profile.setSpecialty(specialty);
        profile.setClinicName(clinicName);
        profile.setClinicAddress(clinicAddress);
        profile.setCity(city);
        profile.setArea(area);
        profile.setPinCode(pinCode);
        profile.setClinicPhone(clinicPhone);
        profile.setPersonalPhone(personalPhone);

        DoctorProfile saved = doctorProfileRepository.save(profile);
        log.info("Successfully updated doctor profile for publicId: {}", doctor.getPublicId());
        return DoctorProfileResponse.fromEntity(saved);
    }

    /**
     * Retrieves a doctor's public profile by their public MediGuide ID.
     * Exposes only safe public fields; never exposes account or internal database IDs.
     *
     * @param publicId unique public identifier of the doctor (e.g. DOC-XXXXXX)
     * @return PublicDoctorProfileResponse
     */
    @Transactional(readOnly = true)
    public PublicDoctorProfileResponse getPublicProfileByPublicId(String publicId) {
        if (publicId == null || publicId.trim().isEmpty()) {
            throw new AppointmentValidationException("Doctor public ID is required");
        }

        DoctorProfile profile = doctorProfileRepository.findByDoctorPublicId(publicId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for public ID: " + publicId));

        return PublicDoctorProfileResponse.fromEntity(profile);
    }

    /**
     * Searches published doctor profiles using optional filters:
     * - Specialty (case-insensitive substring match)
     * - City (case-insensitive substring match)
     * - Area (case-insensitive substring match)
     * - PIN code (exact match)
     *
     * Combines all supplied filters with AND semantics.
     * Safely applies pagination limits (default 20, max 50) to prevent unbounded queries.
     * Returns only public fields safe for patient viewing.
     *
     * @param specialty optional specialty filter
     * @param city optional city filter
     * @param area optional area filter
     * @param pinCode optional PIN code filter
     * @param page zero-based page index
     * @param size page size (capped at MAX_PAGE_SIZE)
     * @return list of PublicDoctorProfileResponse
     */
    @Transactional(readOnly = true)
    public List<PublicDoctorProfileResponse> searchPublicProfiles(
            String specialty, String city, String area, String pinCode, int page, int size) {

        final String trimmedSpecialty = (specialty != null && !specialty.trim().isEmpty()) ? specialty.trim() : null;
        final String trimmedCity = (city != null && !city.trim().isEmpty()) ? city.trim() : null;
        final String trimmedArea = (area != null && !area.trim().isEmpty()) ? area.trim() : null;
        final String trimmedPinCode = (pinCode != null && !pinCode.trim().isEmpty()) ? pinCode.trim() : null;

        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        Specification<DoctorProfile> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (trimmedSpecialty != null) {
                predicates.add(cb.like(cb.lower(root.get("specialty")), "%" + trimmedSpecialty.toLowerCase() + "%"));
            }
            if (trimmedCity != null) {
                predicates.add(cb.like(cb.lower(root.get("city")), "%" + trimmedCity.toLowerCase() + "%"));
            }
            if (trimmedArea != null) {
                predicates.add(cb.like(cb.lower(root.get("area")), "%" + trimmedArea.toLowerCase() + "%"));
            }
            if (trimmedPinCode != null) {
                predicates.add(cb.equal(root.get("pinCode"), trimmedPinCode));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "specialty", "city", "clinicName"));
        Page<DoctorProfile> pageResult = doctorProfileRepository.findAll(spec, pageable);

        return pageResult.getContent().stream()
                .map(PublicDoctorProfileResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
