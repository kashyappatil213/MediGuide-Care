package com.mediguide.service;

import com.mediguide.dto.LoginRequest;
import com.mediguide.dto.LoginResponse;
import com.mediguide.dto.RegisterRequest;
import com.mediguide.dto.UserResponse;
import com.mediguide.exception.DuplicateEmailException;
import com.mediguide.exception.InvalidCredentialsException;
import com.mediguide.exception.InvalidRoleException;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import com.mediguide.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Service handling secure user registration, credential verification, and JWT issuance.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       Validator validator,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new patient or doctor into the system.
     *
     * @param request the registration request details
     * @return a safe UserResponse containing public user information
     * @throws ConstraintViolationException if bean validation fails on the request
     * @throws InvalidRoleException if the role is ADMIN, null, or unsupported for public registration
     * @throws DuplicateEmailException if an account with the normalized email already exists
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }

        if (request.getEmail() != null) {
            request.setEmail(request.getEmail().trim());
        }
        if (request.getFullName() != null) {
            request.setFullName(request.getFullName().trim());
        }

        // 1. Enforce DTO bean validation
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        // 2. Prevent unauthorized role registration (never allow ADMIN publicly)
        if (request.getRole() == null || request.getRole() == Role.ADMIN) {
            throw new InvalidRoleException("Public registration as " +
                    (request.getRole() != null ? request.getRole() : "empty role") +
                    " is not permitted. Only PATIENT and DOCTOR roles are allowed.");
        }

        // 3. Normalize email (trim whitespace and convert to lowercase)
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        // 4. Reject duplicate email registrations
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException("An account with email '" + normalizedEmail + "' already exists");
        }

        // 5. Securely hash password using BCrypt
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 6. Generate role-prefixed, unique public ID with collision retry
        String publicId = generateUniquePublicId(request.getRole());

        // 7. Persist new user entity
        User user = new User(
                request.getFullName().trim(),
                normalizedEmail,
                encodedPassword,
                request.getRole(),
                publicId
        );

        User savedUser = userRepository.save(user);

        // 8. Return safe response including public ID, without password or sensitive hash
        return UserResponse.fromEntity(savedUser);
    }

    /**
     * Generates a unique, role-prefixed public ID using a bounded retry strategy.
     *
     * @param role user role (PATIENT -> PAT-, DOCTOR -> DOC-)
     * @return unique public ID string
     * @throws IllegalStateException if a unique ID cannot be generated after maximum retries
     */
    public String generateUniquePublicId(Role role) {
        final int maxRetries = 5;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String candidate = com.mediguide.util.PublicIdGenerator.generateRandomPublicId(role);
            if (!userRepository.existsByPublicId(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate a unique public ID after " + maxRetries + " attempts");
    }

    /**
     * Authenticates user credentials and generates a signed JWT access token.
     *
     * @param request login credentials
     * @return LoginResponse with access token and safe user details
     * @throws ConstraintViolationException if bean validation fails
     * @throws InvalidCredentialsException if email is not found or password does not match
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request cannot be null");
        }

        if (request.getEmail() != null) {
            request.setEmail(request.getEmail().trim());
        }

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);

        // Uniform authentication failure message to avoid email enumeration
        if (userOptional.isEmpty()) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userOptional.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        UserResponse userResponse = UserResponse.fromEntity(user);

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                userResponse
        );
    }
}
