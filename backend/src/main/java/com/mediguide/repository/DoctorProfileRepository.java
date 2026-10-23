package com.mediguide.repository;

import com.mediguide.model.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for DoctorProfile entity.
 * Supports specification execution for dynamic, multi-field specialty and location filtering.
 */
@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long>, JpaSpecificationExecutor<DoctorProfile> {

    /**
     * Finds the doctor profile linked to a specific doctor user ID.
     *
     * @param doctorId internal primary key of the doctor user
     * @return optional containing the DoctorProfile if present
     */
    Optional<DoctorProfile> findByDoctorId(Long doctorId);

    /**
     * Finds a doctor profile by the doctor's unique public MediGuide ID (e.g. DOC-XXXXXX).
     *
     * @param publicId public identifier of the doctor
     * @return optional containing the DoctorProfile if present
     */
    Optional<DoctorProfile> findByDoctorPublicId(String publicId);

    /**
     * Checks if a profile already exists for the given doctor ID.
     *
     * @param doctorId internal primary key of the doctor user
     * @return true if a profile exists, false otherwise
     */
    boolean existsByDoctorId(Long doctorId);
}
