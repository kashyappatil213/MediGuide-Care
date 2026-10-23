package com.mediguide.repository;

import com.mediguide.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for User entity database access.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their unique email address.
     *
     * @param email user email to search
     * @return an Optional containing the User if found, or Optional.empty()
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by their unique public ID.
     *
     * @param publicId public human-readable identifier (e.g. "PAT-7K4M92" or "DOC-3P8N61")
     * @return an Optional containing the User if found, or Optional.empty()
     */
    Optional<User> findByPublicId(String publicId);

    /**
     * Checks if a public ID already exists in the system.
     *
     * @param publicId candidate public identifier
     * @return true if public ID already assigned, false otherwise
     */
    boolean existsByPublicId(String publicId);

    /**
     * Retrieves all users currently lacking a public ID.
     * Used for safe startup backfill on legacy databases.
     *
     * @return list of users without a public ID
     */
    java.util.List<User> findByPublicIdIsNull();

    /**
     * Obtains an exclusive pessimistic write lock on a user row.
     * Used by appointment booking to serialize bookings per doctor and eliminate double-booking race conditions.
     *
     * @param id user primary key
     * @return an Optional containing the User if found, with pessimistic write lock held
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}

