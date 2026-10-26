# MediGuide Care — Feature Inventory & Implementation Status

This document provides a comprehensive inventory of all features in the MediGuide Care Doctor–Patient Consultation and Follow-up Management System, their verified implementation status, functional capabilities, and known limitations.

---

## 📊 Status Classification
- **[Implemented & Verified]**: Implemented in frontend and backend; verified with automated unit/integration tests and/or UI builds.
- **[Partially Implemented]**: Core logic exists, but secondary capabilities or third-party integrations (e.g. SMS delivery) are deferred.
- **[Planned]**: Architectural hooks or future roadmap items not yet implemented.
- **[Not Yet Verified in Live Production]**: Verified via automated mocks/H2 tests, but not yet tested against a live production cloud deployment.

---

## 📋 Comprehensive Feature Matrix

| # | Feature / Capability | Layer(s) | Status | Test Coverage |
|---|---|---|---|---|
| 1 | **System Health & Connectivity Verification** | Full-Stack | [Implemented & Verified] | Automated Unit & Integration Tests |
| 2 | **User Registration & Email-Based Authentication** | Full-Stack | [Implemented & Verified] | Unit & Integration Tests (BCrypt, Role checks) |
| 3 | **JWT Authentication & Stateless Session Handling** | Backend/Frontend | [Implemented & Verified] | Token generation, validation, expiration tests |
| 4 | **Role-Based Access Control (RBAC: PATIENT / DOCTOR)** | Backend/Frontend | [Implemented & Verified] | Method-level security (`@PreAuthorize`) & UI routing |
| 5 | **Unique Public MediGuide IDs (`DOC-` / `PAT-`)** | Full-Stack | [Implemented & Verified] | Collision-free generation & Backfill runner tests |
| 6 | **Doctor Profile Management & Location Discovery** | Full-Stack | [Implemented & Verified] | Specialty, city, area, PIN code search tests |
| 7 | **Doctor Availability Calendar & Multi-Slot Creation** | Full-Stack | [Implemented & Verified] | Slot creation, overlap detection, deactivation tests |
| 8 | **Patient Appointment Booking & Double-Booking Prevention** | Full-Stack | [Implemented & Verified] | Concurrency tests, slot reservation, state transitions |
| 9 | **Appointment Lifecycle (BOOKED → COMPLETED / CANCELLED)** | Full-Stack | [Implemented & Verified] | Status transitions & caller authorization tests |
| 10 | **Clinical Consultation Records & Digital Prescriptions** | Full-Stack | [Implemented & Verified] | Diagnosis, symptoms, medication dosage/frequency tests |
| 11 | **Consultation History Timeline & Historical Queues** | Full-Stack | [Implemented & Verified] | Patient & doctor chronological consultation queries |
| 12 | **AI-Assisted Clinical History Summarizer** | Full-Stack | [Implemented & Verified] | Structured prompts, doctor-in-the-loop disclosure tests |
| 13 | **Patient Contact Details (Private Phone Number)** | Full-Stack | [Implemented & Verified] | Private view/update tests, isolation & RBAC enforcement |
| 15 | **Email Verification & Notification Foundation** | Full-Stack | [Implemented & Verified] | Unit & Controller tests (token hashing, expiry, rate limits, anti-enumeration) |
| 16 | **QR-Code Based Health Summary Sharing** | Frontend | [Planned] | Roadmap item for mobile client phase |
| 17 | **Mobile App & Responsive Layout Polish** | Frontend | [Partially Implemented] | Desktop-optimized with responsive flexbox/grid foundations |

---

## 🔍 Detailed Feature Breakdown

### 1. User Registration, Authentication & RBAC
- **Login Identifier:** Email address (case-insensitive, normalized).
- **Password Security:** BCrypt password hashing with high work factor (`PasswordConfig`). Passwords are never logged or returned in responses.
- **Access Tokens:** Deterministic HMAC-SHA256 JWT tokens with configurable expiration (default 15 minutes).
- **Role Restrictions:**
  - `ROLE_PATIENT`: Access to appointment booking, doctor search, consultation history, and personal contact profile. Cannot access doctor calendars or patient queue drawers.
  - `ROLE_DOCTOR`: Access to availability slot generation, consultation queue, drawer documentation, digital Rx, and AI summarizer. Cannot register as an admin or access unrelated patient profiles.
- **Admin Self-Registration:** Explicitly prohibited; roles are strictly limited to `PATIENT` and `DOCTOR`.

### 2. Public MediGuide IDs
- **Format:** 
  - Doctors: `DOC-XXXXXX` (e.g., `DOC-4A8K9Z`)
  - Patients: `PAT-XXXXXX` (e.g., `PAT-9B2M1Q`)
- **Privacy:** Public IDs replace internal database auto-incrementing integer IDs in client-facing APIs and UI banners, preventing sequential database enumeration attacks.
- **Backfill Runner:** `PublicIdBackfillRunner` executes automatically upon application startup to populate public IDs for any legacy database records lacking one.

### 3. Doctor Profiles & Specialty/Location Search
- **Profile Fields:**
  - `specialty` (e.g., "Cardiology", "Pediatrics")
  - `clinicName` (e.g., "Apex Heart Hospital")
  - `clinicAddress` (Physical address)
  - `city` (e.g., "Bengaluru", "Mumbai")
  - `area` (e.g., "Indiranagar", "Bandra West")
  - `pinCode` (Validated 6-digit Indian postal code, e.g. "560038")
- **Search Capabilities:**
  - Case-insensitive substring matching for specialty, city, and area.
  - Exact match for PIN code.
  - AND combination logic across all active filter fields.
  - Direct lookup by public ID: `GET /api/v1/doctors/{publicId}`.

### 4. Availability Scheduling & Conflict Prevention
- **Calendar-Based Availability Planner:** Doctors view their schedule and can create multiple consultation time slots for any date.
- **Validation:** Ensures start time is strictly before end time and slots are scheduled in the future.
- **Overlap Prevention:** Prevents creating conflicting availability intervals for the same doctor.
- **Deactivation:** Doctors can deactivate unbooked availability slots. Deactivated slots are excluded from patient discovery.

### 5. Appointment Booking & Lifecycle Management
- **Workflow:**
  1. Patient searches doctors or browses active available slots.
  2. Patient selects a slot and requests an appointment time range within that slot.
  3. Backend locks the slot and creates an appointment in `BOOKED` status.
  4. Double-booking prevention via pessimistic/concurrency validation ensures two patients cannot book the same slot simultaneously.
- **Status Transitions:**
  - `BOOKED` → `COMPLETED` (Completed by the consulting doctor upon submitting consultation notes).
  - `BOOKED` → `CANCELLED` (Can be cancelled by either the patient or doctor prior to consultation).

### 6. Clinical Documentation & Digital Prescriptions
- **Doctor Consultation Drawer:** When attending an appointment, the doctor opens a slide-over clinical drawer to document:
  - Symptoms and clinical observations
  - Diagnosis
  - Treatment advice
  - Recommended follow-up date
- **Structured Digital Prescriptions:**
  - Medication name
  - Dosage (e.g., "500 mg")
  - Frequency (e.g., "Twice daily after meals")
  - Duration (e.g., "5 days")
- Submitting consultation notes automatically marks the appointment as `COMPLETED`.

### 7. Consultation History & Care Timeline
- **Patient View:** Chronological record of all past consultations, diagnoses, prescribed medications, and follow-up guidance.
- **Doctor View:** Access to historical consultation records for patients who have appointments with that doctor.

### 8. AI-Assisted Clinical History Summarizer
- **Purpose:** Generates a concise, structured draft summary of a patient's historical medical consultations to assist the doctor during consultations.
- **Safety & Doctor-in-the-Loop:**
  - Doctors must explicitly accept a clinical disclosure acknowledgement before generating a summary.
  - Summaries are presented strictly as reviewable clinical drafts with a disclaimer that AI output does not constitute medical advice and must be verified by the physician.
  - Copy-to-clipboard functionality allows doctors to incorporate parts into consultation notes after verification.
- **Architecture & Privacy:**
  - Backend integration via `ClinicalSummaryAiClient` and `OpenAiCompatibleClinicalSummaryClient`.
  - Configurable OpenAI-compatible provider (`AI_API_KEY`, `AI_BASE_URL`, `AI_MODEL`).
  - Sensitive internal database IDs and personal identifiers are sanitized; only relevant chronological clinical data (symptoms, diagnoses, treatments, medications) are supplied in the prompt.
  - Character limit protection (`AI_MAX_INPUT_CHARS`) guards against prompt injection and excessive payload sizes.

### 9. Patient & Doctor Contact Details (Task 1 Improvements)
- **Patient Contact Number:**
  - Stored in `users.phone_number`.
  - Strictly private: Accessible and editable only by the authenticated patient via `GET /api/v1/patient/profile` and `PUT /api/v1/patient/profile`.
  - Excluded from all doctor discovery and public endpoints.
- **Doctor Contact Details:**
  - **Clinic Contact Number:** Stored in `doctor_profiles.clinic_phone`. Visible to patients on published doctor profiles and search results for clinic inquiries.
  - **Personal Phone Number:** Stored in `doctor_profiles.personal_phone`. Strictly confidential; returned only to the doctor in `DoctorProfileResponse` and excluded from `PublicDoctorProfileResponse` and public search.
- **Format Validation:** Flexible regex pattern `^[+]?[0-9\s\-()]{7,20}$` supports domestic and international formats without enforcing a single country code.

### 10. Email Verification & Notification Foundation (Feature 6)
- **Email Delivery Integration:** Powered by Spring Boot Starter Mail (`JavaMailSender`) with configurable SMTP environment variables (`SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`, `FRONTEND_URL`).
- **Cryptographic Token Lifecycle:**
  - High-entropy 32-byte Base64URL random token generated via `SecureRandom`.
  - Stored strictly as a 64-character SHA-256 hash in `email_verification_tokens`. Raw plaintext tokens are never persisted in the database.
  - 24-hour expiration window; expired tokens are cleaned up upon detection.
  - Strict single-use consumption: tokens are permanently deleted immediately upon successful verification.
- **Controlled Resend & Rate Limiting:**
  - 60-second cooldown per user account; requests within cooldown return HTTP `429 Too Many Requests` with a standard `Retry-After` header.
  - Anti-enumeration defense: requests for non-existent or already-verified emails return the identical success message ("If an unverified account exists for that email, a verification link has been sent").
- **Progressive Access Policy (Soft Enforcement):**
  - Unverified accounts can log in with full credentials and view read-only dashboard data.
  - Portals display clear warning banners indicating unverified status with an inline "✉ Resend Email" button.
  - High-trust actions are strictly protected:
    - Patients cannot book appointments without a verified email (`AppointmentService`).
    - Doctors cannot publish availability schedules without a verified email (`DoctorAvailabilityService`).
- **Zero Token Leakage:** Verification tokens are never returned in JSON registration responses, login payloads, or logged to application output.
- **Deterministic Testing:** Automated tests run against `TestMailConfig` mock `JavaMailSender`, preventing outbound network traffic and ensuring 100% test reliability.

---

## ⚠️ Known Limitations & Current Operational Constraints

1. **Email SMTP Provider Limits:** Real-world dispatch depends on external SMTP providers (such as Gmail, Amazon SES, or SendGrid). For example, Gmail SMTP requires a 16-character Google App Password (with 2-Step Verification enabled) and enforces standard sending quotas (typically 500 emails/day for free accounts). Local automated tests use mock `JavaMailSender` and do not require internet or live credentials.
2. **Asynchronous Retry Queues:** Email verification dispatch is synchronous within registration and resend transactions, protected by graceful try-catch degradation (SMTP failure logs a warning and does not roll back user registration). Distributed message queues (RabbitMQ / Kafka / Celery) and automated dead-letter retries are deferred to subsequent enterprise scaling sprints.
3. **Third-Party AI API Dependency:** The AI summarizer requires an active internet connection and valid API key when running against external providers like OpenAI. In development/testing, an in-memory fallback/mock is used.
4. **Database Schema Strategy:** Development uses Hibernate `ddl-auto=update`. In enterprise production, Flyway or Liquibase migrations should be configured.
5. **Mobile Responsiveness:** The current UI is fully usable on desktop and modern tablet viewports; comprehensive mobile responsiveness optimization is slated for the dedicated mobile sprint.
