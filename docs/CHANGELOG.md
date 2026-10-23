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
