package com.mediguide.repository;

import com.mediguide.model.ConsultationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ConsultationRecord entity database access.
 */
@Repository
public interface ConsultationRecordRepository extends JpaRepository<ConsultationRecord, Long> {

    /**
     * Finds the consultation record linked to a specific appointment ID.
     *
     * @param appointmentId appointment primary key
     * @return Optional containing the consultation record if present
     */
    Optional<ConsultationRecord> findByAppointmentId(Long appointmentId);

    /**
     * Checks if a consultation record already exists for the given appointment ID.
     *
     * @param appointmentId appointment primary key
     * @return true if a consultation record exists, false otherwise
     */
    boolean existsByAppointmentId(Long appointmentId);

    /**
     * Retrieves all consultation records for a patient, ordered chronologically newest first.
     * Uses JOIN FETCH on appointment, doctor, and patient to eliminate N+1 queries.
     *
     * @param patientId primary key of the patient user
     * @return list of consultation records
     */
    @Query("SELECT c FROM ConsultationRecord c " +
           "JOIN FETCH c.appointment a " +
           "JOIN FETCH a.doctor d " +
           "JOIN FETCH a.patient p " +
           "WHERE p.id = :patientId " +
           "ORDER BY a.startTime DESC, c.createdAt DESC")
    List<ConsultationRecord> findHistoryByPatientId(@Param("patientId") Long patientId);

    /**
     * Retrieves all consultation records conducted by a doctor, ordered chronologically newest first.
     * Uses JOIN FETCH on appointment, doctor, and patient to eliminate N+1 queries.
     *
     * @param doctorId primary key of the doctor user
     * @return list of consultation records
     */
    @Query("SELECT c FROM ConsultationRecord c " +
           "JOIN FETCH c.appointment a " +
           "JOIN FETCH a.doctor d " +
           "JOIN FETCH a.patient p " +
           "WHERE d.id = :doctorId " +
           "ORDER BY a.startTime DESC, c.createdAt DESC")
    List<ConsultationRecord> findHistoryByDoctorId(@Param("doctorId") Long doctorId);

    /**
     * Retrieves all consultation records between a specific doctor and patient, ordered chronologically newest first.
     * Uses JOIN FETCH on appointment, doctor, and patient to eliminate N+1 queries.
     *
     * @param doctorId primary key of the doctor user
     * @param patientId primary key of the patient user
     * @return list of consultation records
     */
    @Query("SELECT c FROM ConsultationRecord c " +
           "JOIN FETCH c.appointment a " +
           "JOIN FETCH a.doctor d " +
           "JOIN FETCH a.patient p " +
           "WHERE d.id = :doctorId AND p.id = :patientId " +
           "ORDER BY a.startTime DESC, c.createdAt DESC")
    List<ConsultationRecord> findHistoryByDoctorIdAndPatientId(
            @Param("doctorId") Long doctorId,
            @Param("patientId") Long patientId);
}

