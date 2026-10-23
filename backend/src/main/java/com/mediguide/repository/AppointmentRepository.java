package com.mediguide.repository;

import com.mediguide.model.Appointment;
import com.mediguide.model.AppointmentStatus;
import com.mediguide.model.DoctorAvailability;
import com.mediguide.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Appointment entity database access.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {


    /**
     * Finds all appointments booked by a specific patient entity.
     *
     * @param patient patient user
     * @return list of patient appointments
     */
    List<Appointment> findByPatient(User patient);

    /**
     * Finds all appointments booked by patient ID.
     *
     * @param patientId primary key of the patient user
     * @return list of patient appointments
     */
    List<Appointment> findByPatientId(Long patientId);

    /**
     * Finds all appointments assigned to a specific doctor entity.
     *
     * @param doctor doctor user
     * @return list of doctor appointments
     */
    List<Appointment> findByDoctor(User doctor);

    /**
     * Finds all appointments assigned to a doctor ID.
     *
     * @param doctorId primary key of the doctor user
     * @return list of doctor appointments
     */
    List<Appointment> findByDoctorId(Long doctorId);

    /**
     * Finds all appointments linked to a specific doctor availability slot.
     *
     * @param availability availability slot entity
     * @return list of appointments within that slot
     */
    List<Appointment> findByAvailability(DoctorAvailability availability);

    /**
     * Finds all appointments linked to an availability slot ID.
     *
     * @param availabilityId primary key of the availability slot
     * @return list of appointments within that slot
     */
    List<Appointment> findByAvailabilityId(Long availabilityId);

    /**
     * Finds all appointments for a doctor filtered by lifecycle status.
     *
     * @param doctorId primary key of the doctor user
     * @param status appointment lifecycle status
     * @return list of matching appointments
     */
    List<Appointment> findByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);

    /**
     * Finds all appointments for a patient filtered by lifecycle status.
     *
     * @param patientId primary key of the patient user
     * @param status appointment lifecycle status
     * @return list of matching appointments
     */
    List<Appointment> findByPatientIdAndStatus(Long patientId, AppointmentStatus status);

    /**
     * Finds all appointments within an availability slot filtered by lifecycle status.
     *
     * @param availabilityId primary key of the availability slot
     * @param status appointment lifecycle status
     * @return list of matching appointments
     */
    List<Appointment> findByAvailabilityIdAndStatus(Long availabilityId, AppointmentStatus status);

    /**
     * Checks if any appointment relationship exists between a specific doctor and patient.
     * Used for doctor-patient clinical access authorization boundaries.
     *
     * @param doctorId primary key of the doctor user
     * @param patientId primary key of the patient user
     * @return true if an appointment relationship exists, false otherwise
     */
    boolean existsByDoctorIdAndPatientId(Long doctorId, Long patientId);

    /**
     * Finds overlapping BOOKED appointments for a doctor across any availability record
     * within the half-open interval [startTime, endTime).
     * Two intervals overlap when: requestedStart < existingEnd AND requestedEnd > existingStart.
     *
     * @param doctorId primary key of the doctor
     * @param startTime requested start time
     * @param endTime requested end time
     * @return list of conflicting booked appointments
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.status = com.mediguide.model.AppointmentStatus.BOOKED AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Appointment> findOverlappingBookedAppointmentsForDoctor(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Finds overlapping BOOKED appointments for a patient across any doctor
     * within the half-open interval [startTime, endTime).
     * Two intervals overlap when: requestedStart < existingEnd AND requestedEnd > existingStart.
     *
     * @param patientId primary key of the patient
     * @param startTime requested start time
     * @param endTime requested end time
     * @return list of conflicting booked appointments
     */
    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId AND a.status = com.mediguide.model.AppointmentStatus.BOOKED AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Appointment> findOverlappingBookedAppointmentsForPatient(
            @Param("patientId") Long patientId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Finds future BOOKED appointments associated with a doctor availability slot.
     * Used to prevent deactivation of availability intervals when upcoming patient bookings depend on it.
     *
     * @param availabilityId primary key of the availability slot
     * @param now current threshold timestamp
     * @return list of future booked appointments
     */
    @Query("SELECT a FROM Appointment a WHERE a.availability.id = :availabilityId AND a.status = com.mediguide.model.AppointmentStatus.BOOKED AND a.endTime > :now")
    List<Appointment> findFutureBookedAppointmentsByAvailabilityId(
            @Param("availabilityId") Long availabilityId,
            @Param("now") LocalDateTime now);

    /**
     * Obtains an exclusive pessimistic write lock on an appointment row.
     * Used by appointment cancellation and completion to serialize status transitions.
     *
     * @param id appointment primary key
     * @return an Optional containing the Appointment if found, with pessimistic write lock held
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") Long id);
}

