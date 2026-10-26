package com.mediguide.repository;

import com.mediguide.model.EmailVerificationToken;
import com.mediguide.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for EmailVerificationToken entities.
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    Optional<EmailVerificationToken> findByUser(User user);

    Optional<EmailVerificationToken> findByUserId(Long userId);

    void deleteByUser(User user);
}
