# MediGuide Care — Feature Progression & Changelog

This changelog records the chronological development progression of MediGuide Care from initial project creation to the current release, based on available codebase milestones and verified functionality.

---

## [Phase 1] — Project Foundation & Health Verification
- **Milestone:** Initial project scaffolding.
- **Backend:** Configured Java 17, Spring Boot 3.2, Maven, PostgreSQL JDBC driver, Spring Data JPA, and springdoc-openapi.
- **Frontend:** Scaffolding with React 19, Vite, and custom CSS design system.
- **Health Verification:** Implemented `/api/v1/health` with database ping and diagnostic dashboard.

---

## [Phase 2] — Authentication, JWT Security & Role Boundaries
- **User Entity & Security:** Implemented `User` model with `Role` (`PATIENT`, `DOCTOR`).
- **Password Security:** Integrated BCrypt hashing via `PasswordEncoder` (`PasswordConfig`).
- **Stateless Tokens:** Implemented `JwtService` and `JwtAuthenticationFilter` with HMAC-SHA256 signing and expiration validation.
- **Role-Based Access Control:** Configured method-level security with `@PreAuthorize`, `CustomAuthenticationEntryPoint`, and `CustomAccessDeniedHandler`.
- **Frontend AuthCard:** Login and registration forms with validation and protected connection verification.

---

## [Phase 3] — Doctor Scheduling & Appointment Lifecycle
- **Doctor Availability:** Calendar-based availability scheduler supporting multiple slots per day and slot deactivation (`DoctorAvailability`).
- **Appointment Booking:** Patient appointment booking with start/end time validation within doctor's availability intervals.
- **Conflict Prevention:** Pessimistic/transactional overlap detection preventing double-booking of doctors or overlapping patient appointments.
- **Appointment Lifecycle:** Status transitions from `BOOKED` to `COMPLETED` or `CANCELLED` with authorization checks.

---

## [Phase 4] — Clinical Documentation, Prescriptions & Timeline
- **Consultation Records:** Clinical drawer allowing attending doctors to document symptoms, diagnoses, treatment advice, and recommended follow-up dates (`ConsultationRecord`).
- **Digital Prescriptions:** Multi-item prescription model with medication name, dosage, frequency, and duration (`Prescription`).
- **Consultation History:** Historical timeline queue enabling patients to view their complete clinical history and authorized doctors to view past consultations for queued patients.

---

## [Feature 4] — Unique Public Doctor and Patient IDs
- **Public ID Generator:** Introduced collision-resistant `DOC-XXXXXX` and `PAT-XXXXXX` alphanumeric identifiers.
- **Database Backfill:** Implemented `PublicIdBackfillRunner` executing at startup to assign public IDs to any existing records lacking one.
- **Security Boundary:** Replaced internal database integer IDs in all client-facing responses, preventing enumeration attacks.

---

## [Feature 5] — Doctor Profiles & Specialty/Location Search
- **Doctor Profile Model:** Added `DoctorProfile` storing specialty, clinic name, clinic address, city, area, and 6-digit PIN code.
- **Search Engine:** Dynamic multi-field search allowing patients to find doctors by specialty, city, area, or PIN code with case-insensitive substring and exact PIN filtering.
- **Public Doctor Lookups:** Public doctor detail endpoint (`/api/v1/doctors/{publicId}`) exposing clinic details while strictly excluding doctor emails, passwords, and database IDs.

---

## [AI Feature] — AI-Assisted Clinical History Summarizer
- **Client Integration:** Implemented `ClinicalSummaryAiClient` and `OpenAiCompatibleClinicalSummaryClient` with configurable endpoint, model, and timeout settings.
- **Structured Prompting:** Formats historical consultations into safe, chronological summaries highlighting recurring diagnoses and active medications.
- **Doctor Disclosure & Verification:** Slide-over disclosure confirmation enforcing that summaries are unverified clinical drafts requiring physician review.
- **Clipboard Utility:** One-click copy tool allowing doctors to transfer verified summary excerpts into consultation notes.

---

## [Task 1] — Individual Profile Improvements (Latest)
- **Patient Contact Profile:**
  - Added private `phoneNumber` to `User` entity (`users.phone_number`).
  - Added dedicated endpoints `GET /api/v1/patient/profile` and `PUT /api/v1/patient/profile` restricted to `PATIENT` role.
  - Added "👤 My Contact Profile" section in patient portal.
- **Doctor Contact Profile:**
  - Added `clinicPhone` (public on clinic profile & search) and `personalPhone` (strictly private to doctor) in `doctor_profiles`.
  - Updated doctor portal profile management form to configure both numbers.
  - Added clinic phone display on patient doctor search result cards.
  - Verified that doctor personal phone numbers are strictly excluded from public doctor profiles and search.

---

## [Feature 6] — Email Verification & Notification Foundation (Latest)
- **Email Verification Architecture:**
  - Replaced planned SMS notifications with email verification and notification foundation using `spring-boot-starter-mail` and configurable SMTP settings.
  - Added `email_verified` flag to `User` entity (`users.email_verified`), defaulting to `true` on existing accounts and test fixtures to preserve backward compatibility, and set to `false` on new user registration.
  - Implemented `EmailVerificationToken` entity storing 64-character SHA-256 token hashes, creation timestamps, expiration timestamps, and last-sent timestamps. Raw plaintext tokens are never persisted in the database.
- **Verification & Resend Endpoints:**
  - `POST /api/v1/auth/verify-email`: Validates raw token hash against the database, sets user `email_verified = true`, and enforces single-use behavior by deleting the consumed token immediately.
  - `POST /api/v1/auth/resend-verification`: Dispatches a fresh verification token. Enforces rate-limiting cooldown (default 60 seconds) returning HTTP `429 Too Many Requests` with a `Retry-After` header.
- **Anti-Enumeration & Security Guarantees:**
  - Uniform response semantics for non-existent or already-verified accounts ("If an unverified account exists for that email, a verification link has been sent") prevents account enumeration.
  - Verification tokens are never logged or exposed in API registration responses or user profiles.
  - Deterministic test execution via `TestMailConfig` mock `JavaMailSender` prevents outbound network calls during automated test suites.
- **Progressive Access Policy (Soft Enforcement):**
  - Unverified patients and doctors can log in to view portal features and are presented with an unverified email alert banner containing an inline "✉ Resend Email" button.
  - High-trust actions are strictly protected: unverified patients cannot book appointments (`AppointmentService`), and unverified doctors cannot publish availability schedules (`DoctorAvailabilityService`).
- **Frontend Integration:**
  - `AuthCard.jsx`: Detects `?verify-email=<token>` URL query parameter, triggers automatic verification with clear success/error banners, cleans the browser URL history, and provides a collapsible "Need another verification link?" section.
  - `PatientPortal.jsx` & `DoctorPortal.jsx`: Added responsive warning alert banners for unverified accounts with inline single-click resend capabilities.
- **Automated Test Suite Expansion:**
  - Added `EmailVerificationServiceTest` (9 unit tests) and `EmailVerificationControllerTest` (8 integration tests).
  - Total automated backend tests increased from 281 to 298 passing tests (100% pass rate).
