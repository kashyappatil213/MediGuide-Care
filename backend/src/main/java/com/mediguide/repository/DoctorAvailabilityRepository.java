package com.mediguide.repository;

import com.mediguide.model.DoctorAvailability;
import com.mediguide.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for DoctorAvailability entity database access.
 */
@Repository
public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {

    /**
     * Finds all availability slots for a given doctor entity.
     *
     * @param doctor doctor user
     * @return list of availability intervals
     */
    List<DoctorAvailability> findByDoctor(User doctor);

    /**
     * Finds all availability slots by doctor ID.
     *
     * @param doctorId primary key of the doctor user
     * @return list of availability intervals
     */
    List<DoctorAvailability> findByDoctorId(Long doctorId);

    /**
     * Finds all active availability slots for a doctor.
     *
     * @param doctorId primary key of the doctor user
     * @return list of active availability intervals
     */
    List<DoctorAvailability> findByDoctorIdAndActiveTrue(Long doctorId);

    /**
     * Finds all globally active availability slots.
     *
     * @return list of active availability intervals
     */
    List<DoctorAvailability> findByActiveTrue();

    /**
     * Finds upcoming availability slots for a doctor starting after a given timestamp.
     *
     * @param doctorId primary key of the doctor user
     * @param after lower bound timestamp
     * @return list of upcoming availability intervals
     */
    List<DoctorAvailability> findByDoctorIdAndStartTimeAfter(Long doctorId, LocalDateTime after);

    /**
     * Finds all availability slots for a doctor ordered chronologically by start time.
     *
     * @param doctorId primary key of the doctor user
     * @return chronologically sorted availability slots
     */
    List<DoctorAvailability> findByDoctorIdOrderByStartTimeAsc(Long doctorId);

    /**
     * Finds all active availability slots for a doctor ordered chronologically by start time.
     *
     * @param doctorId primary key of the doctor user
     * @return chronologically sorted active availability slots
     */
    List<DoctorAvailability> findByDoctorIdAndActiveTrueOrderByStartTimeAsc(Long doctorId);

    /**
     * Finds all active future availability slots across all doctors eligible for booking,
     * ordered chronologically by start time.
     * Excludes inactive slots and slots whose end time is in the past.
     *
     * @param now current threshold timestamp
     * @return chronologically sorted list of active future availability intervals
     */
    @Query("SELECT da FROM DoctorAvailability da WHERE da.active = true AND da.endTime > :now ORDER BY da.startTime ASC")
    List<DoctorAvailability> findActiveFutureAvailabilities(@Param("now") LocalDateTime now);

    /**
     * Finds active availability intervals for a doctor that overlap with the specified interval.
     * Two intervals overlap when: requestedStart < existingEnd AND requestedEnd > existingStart.
     *
     * @param doctorId primary key of the doctor user
     * @param startTime requested start timestamp
     * @param endTime requested end timestamp
     * @return list of overlapping active availability intervals
     */
    @Query("SELECT da FROM DoctorAvailability da WHERE da.doctor.id = :doctorId AND da.active = true " +
           "AND da.startTime < :endTime AND da.endTime > :startTime ORDER BY da.startTime ASC")
    List<DoctorAvailability> findOverlappingActiveAvailabilities(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}


