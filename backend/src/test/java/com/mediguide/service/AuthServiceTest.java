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
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private Validator validator;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        jwtService = new JwtService("test-secret-key-at-least-32-bytes-long-for-hs256-signing-1234567890", 15);
        jwtService.init();
        authService = new AuthService(userRepository, passwordEncoder, validator, jwtService);
    }

    @Test
    @DisplayName("Should successfully register a valid patient and hash their password")
    void shouldRegisterValidPatientSuccessfully() {
        RegisterRequest request = new RegisterRequest("Rohan Verma", "rohan@mediguide.local", "StrongPassword123", Role.PATIENT);

        when(userRepository.findByEmail("rohan@mediguide.local")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(101L);
            return user;
        });

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("Rohan Verma", response.getFullName());
        assertEquals("rohan@mediguide.local", response.getEmail());
        assertEquals(Role.PATIENT, response.getRole());
        assertNotNull(response.getPublicId());
        assertTrue(response.getPublicId().startsWith("PAT-"), "Patient public ID should start with PAT-");

        // Verify password was securely hashed and matches original
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertNotEquals("StrongPassword123", savedUser.getPassword());
        assertTrue(passwordEncoder.matches("StrongPassword123", savedUser.getPassword()));
        assertEquals(response.getPublicId(), savedUser.getPublicId());
    }

    @Test
    @DisplayName("Should successfully register a valid doctor")
    void shouldRegisterValidDoctorSuccessfully() {
        RegisterRequest request = new RegisterRequest("Dr. Priya Rao", "priya.rao@mediguide.local", "DoctorSecurePass2026", Role.DOCTOR);

        when(userRepository.findByEmail("priya.rao@mediguide.local")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(102L);
            return user;
        });

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(102L, response.getId());
        assertEquals(Role.DOCTOR, response.getRole());
        assertNotNull(response.getPublicId());
        assertTrue(response.getPublicId().startsWith("DOC-"), "Doctor public ID should start with DOC-");
    }

    @Test
    @DisplayName("Should retry when public ID collision occurs and succeed within bounded retry attempts")
    void shouldRetryOnPublicIdCollisionAndSucceed() {
        // Return true (collision) for first 2 attempts, then false (available) on 3rd attempt
        when(userRepository.existsByPublicId(any(String.class)))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        String generatedId = authService.generateUniquePublicId(Role.PATIENT);

        assertNotNull(generatedId);
        assertTrue(generatedId.startsWith("PAT-"));
        verify(userRepository, org.mockito.Mockito.times(3)).existsByPublicId(any(String.class));
    }

    @Test
    @DisplayName("Should fail safely with IllegalStateException when bounded collision retries are exhausted")
    void shouldThrowExceptionWhenMaxCollisionsExceeded() {
        // Always collide
        when(userRepository.existsByPublicId(any(String.class))).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                authService.generateUniquePublicId(Role.DOCTOR)
        );

        assertTrue(ex.getMessage().contains("Failed to generate a unique public ID after 5 attempts"));
        verify(userRepository, org.mockito.Mockito.times(5)).existsByPublicId(any(String.class));
    }

    @Test
    @DisplayName("Response DTO must not expose password or password hash fields")
    void responseDtoMustNotExposePasswordOrHash() {
        Field[] fields = UserResponse.class.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName().toLowerCase();
            assertNotEquals("password", name, "UserResponse should never contain a password field");
            assertNotEquals("hash", name, "UserResponse should never contain a hash field");
        }
    }

    @Test
    @DisplayName("Should reject registration when normalized email already exists")
    void shouldRejectDuplicateNormalizedEmail() {
        RegisterRequest request = new RegisterRequest("Rahul Sen", "  RAHUL.SEN@MEDIGUIDE.LOCAL  ", "SecretPassword99", Role.PATIENT);

        User existingUser = new User("Rahul Existing", "rahul.sen@mediguide.local", "existing_hash", Role.PATIENT);
        when(userRepository.findByEmail("rahul.sen@mediguide.local")).thenReturn(Optional.of(existingUser));

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("rahul.sen@mediguide.local"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject public registration with ADMIN role")
    void shouldRejectAdminRegistration() {
        RegisterRequest request = new RegisterRequest("Admin User", "admin@mediguide.local", "AdminSuperPassword1", Role.ADMIN);

        InvalidRoleException ex = assertThrows(InvalidRoleException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("ADMIN"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should normalize email by trimming spaces and converting to lowercase")
    void shouldNormalizeEmailCorrectly() {
        RegisterRequest request = new RegisterRequest("Ananya Roy", "   Ananya.Roy@MediGuide.LOCAL   ", "ValidPassword123", Role.PATIENT);

        when(userRepository.findByEmail("ananya.roy@mediguide.local")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(request);

        assertEquals("ananya.roy@mediguide.local", response.getEmail());
        verify(userRepository).findByEmail("ananya.roy@mediguide.local");
    }

    @Test
    @DisplayName("Should reject blank full name through Bean Validation")
    void shouldRejectBlankFullName() {
        RegisterRequest request = new RegisterRequest("", "valid@mediguide.local", "Password12345", Role.PATIENT);
        assertThrows(ConstraintViolationException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject invalid email format through Bean Validation")
    void shouldRejectInvalidEmailFormat() {
        RegisterRequest request = new RegisterRequest("Valid Name", "invalid-email-string", "Password12345", Role.PATIENT);
        assertThrows(ConstraintViolationException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject short password less than 8 characters through Bean Validation")
    void shouldRejectShortPassword() {
        RegisterRequest request = new RegisterRequest("Valid Name", "valid@mediguide.local", "short7", Role.PATIENT);
        assertThrows(ConstraintViolationException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject null role through Bean Validation")
    void shouldRejectNullRole() {
        RegisterRequest request = new RegisterRequest("Valid Name", "valid@mediguide.local", "Password12345", null);
        assertThrows(ConstraintViolationException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    // --- Login Tests ---

    @Test
    @DisplayName("Should login successfully with correct credentials and return valid JWT response")
    void shouldLoginSuccessfullyWithCorrectCredentials() {
        String rawPassword = "CorrectPassword123";
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User("Dr. Aisha", "aisha@mediguide.local", hashedPassword, Role.DOCTOR, "DOC-998877");
        user.setId(501L);

        when(userRepository.findByEmail("aisha@mediguide.local")).thenReturn(Optional.of(user));

        LoginRequest loginRequest = new LoginRequest("aisha@mediguide.local", rawPassword);
        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900L, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals(501L, response.getUser().getId());
        assertEquals("Dr. Aisha", response.getUser().getFullName());
        assertEquals("aisha@mediguide.local", response.getUser().getEmail());
        assertEquals(Role.DOCTOR, response.getUser().getRole());
        assertEquals("DOC-998877", response.getUser().getPublicId());

        // Verify token parses correctly
        var claims = jwtService.parseAndValidateToken(response.getAccessToken());
        assertEquals("aisha@mediguide.local", claims.getSubject());
        assertEquals("DOCTOR", claims.get("role"));
        assertEquals(501, claims.get("userId", Integer.class));
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException for non-existent email")
    void shouldThrowInvalidCredentialsForUnknownEmail() {
        when(userRepository.findByEmail("unknown@mediguide.local")).thenReturn(Optional.empty());

        LoginRequest loginRequest = new LoginRequest("unknown@mediguide.local", "SomePassword123");
        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw same generic InvalidCredentialsException for wrong password")
    void shouldThrowInvalidCredentialsForIncorrectPassword() {
        String hashedPassword = passwordEncoder.encode("RealPassword123");
        User user = new User("Test Patient", "patient@mediguide.local", hashedPassword, Role.PATIENT);
        user.setId(502L);

        when(userRepository.findByEmail("patient@mediguide.local")).thenReturn(Optional.of(user));

        LoginRequest loginRequest = new LoginRequest("patient@mediguide.local", "WrongPassword999");
        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    @DisplayName("Should normalize email with whitespace and mixed casing during login")
    void shouldNormalizeEmailDuringLogin() {
        String hashedPassword = passwordEncoder.encode("Password123");
        User user = new User("Test User", "normalized@mediguide.local", hashedPassword, Role.PATIENT);
        user.setId(503L);

        when(userRepository.findByEmail("normalized@mediguide.local")).thenReturn(Optional.of(user));

        LoginRequest loginRequest = new LoginRequest("   NORMALIZED@MediGuide.LOCAL   ", "Password123");
        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        verify(userRepository).findByEmail("normalized@mediguide.local");
    }

    @Test
    @DisplayName("LoginResponse must not expose password or hash fields")
    void loginResponseMustNotExposePasswordOrHash() {
        Field[] fields = LoginResponse.class.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName().toLowerCase();
            assertNotEquals("password", name, "LoginResponse should never contain a password field");
            assertNotEquals("hash", name, "LoginResponse should never contain a hash field");
        }
    }

    @Test
    @DisplayName("Should reject blank login email through Bean Validation")
    void shouldRejectBlankLoginEmail() {
        LoginRequest loginRequest = new LoginRequest("", "Password123");
        assertThrows(ConstraintViolationException.class, () -> authService.login(loginRequest));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Should reject blank login password through Bean Validation")
    void shouldRejectBlankLoginPassword() {
        LoginRequest loginRequest = new LoginRequest("valid@mediguide.local", "");
        assertThrows(ConstraintViolationException.class, () -> authService.login(loginRequest));
        verify(userRepository, never()).findByEmail(any());
    }
}
