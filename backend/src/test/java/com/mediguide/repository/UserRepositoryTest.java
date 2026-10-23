package com.mediguide.repository;

import com.mediguide.model.Role;
import com.mediguide.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should persist a new user and retrieve successfully by email")
    void shouldSaveAndFindByEmail() {
        String testEmail = "doctor.test" + System.currentTimeMillis() + "@mediguide.local";
        User user = new User("Dr. Aisha Sharma", testEmail, "hashed_pw_placeholder_123", Role.DOCTOR);

        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getId());
        assertEquals("Dr. Aisha Sharma", savedUser.getFullName());
        assertEquals(Role.DOCTOR, savedUser.getRole());

        Optional<User> foundOptional = userRepository.findByEmail(testEmail);
        assertTrue(foundOptional.isPresent());
        User foundUser = foundOptional.get();
        assertEquals(savedUser.getId(), foundUser.getId());
        assertEquals(testEmail, foundUser.getEmail());
        assertEquals(Role.DOCTOR, foundUser.getRole());
    }

    @Test
    @DisplayName("Should return empty optional when queried email does not exist")
    void shouldReturnEmptyForNonExistentEmail() {
        Optional<User> result = userRepository.findByEmail("nonexistent_user@mediguide.local");
        assertFalse(result.isPresent());
    }
}
