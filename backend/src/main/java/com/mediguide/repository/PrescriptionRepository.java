package com.mediguide.repository;

import com.mediguide.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA repository for Prescription entity database access.
 */
@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    /**
     * Finds all prescription items prescribed for a specific appointment ID, sorted chronologically.
     *
     * @param appointmentId appointment primary key
     * @return list of prescriptions for this appointment
     */
    List<Prescription> findByAppointmentIdOrderByCreatedAtAsc(Long appointmentId);

    /**
     * Finds all prescription items for an appointment ID.
     *
     * @param appointmentId appointment primary key
     * @return list of prescriptions
     */
    List<Prescription> findByAppointmentId(Long appointmentId);

    /**
     * Batch retrieves all prescription items for a collection of appointment IDs, sorted chronologically.
     * Used by consultation history to eliminate N+1 queries.
     *
     * @param appointmentIds collection of appointment primary keys
     * @return list of matching prescriptions
     */
    List<Prescription> findByAppointmentIdInOrderByCreatedAtAsc(Collection<Long> appointmentIds);
}

