# MediGuide Care — System Architecture & Design Documentation

## 1. System Overview

MediGuide Care is built as a decoupled, multi-tier web application designed for clinic operations:
- **Client Layer:** Single-Page Application (SPA) built with React 19 and Vite, styled using a custom Vanilla CSS design system.
- **API & Security Layer:** Spring Boot 3.2 REST API with Spring Security 6, JWT stateless authentication, and method-level authorization.
- **Persistence Layer:** PostgreSQL 16 relational database mapped via Spring Data JPA and Hibernate ORM.
- **External AI Integration:** Pluggable OpenAI-compatible REST client for assisted clinical history summarization.

---

## 2. High-Level Architecture Diagram

```mermaid
graph TD
    subgraph Client["Client Tier (React 19 + Vite)"]
        UI["React SPA (Port 5173)"]
        AuthComp["AuthCard (Login / Register)"]
        DocPortal["DoctorPortal (Calendar, Queue, Drawer, AI)"]
        PatPortal["PatientPortal (Search, Booking, Timeline)"]
        ApiClient["apiClient.js (JWT Bearer injection)"]
    end

    subgraph Security["Spring Security 6 Gateway (Port 8080)"]
        Cors["CorsFilter (Origin Validation)"]
        JwtFilter["JwtAuthenticationFilter (Bearer token validation)"]
        SecCtx["SecurityContext (Authenticated UserPrincipal)"]
        AuthEntry["CustomAuthenticationEntryPoint & AccessDeniedHandler"]
    end

    subgraph Controllers["REST Controllers Layer"]
        AuthController["AuthController (/api/v1/auth)"]
        DocProfileCtrl["DoctorProfileController (/api/v1/doctor/profile, /api/v1/doctors)"]
        PatProfileCtrl["PatientProfileController (/api/v1/patient/profile)"]
        AvailCtrl["DoctorAvailabilityController (/api/v1/doctor/availability)"]
        ApptCtrl["AppointmentController (/api/v1/appointments)"]
        ConsultCtrl["ConsultationController & ConsultationHistoryController"]
        AiCtrl["ClinicalHistorySummaryController (/api/v1/clinical-summary)"]
    end

    subgraph Services["Service & Business Logic Tier"]
        AuthSvc["AuthService (BCrypt hashing, User creation)"]
        JwtSvc["JwtService (HMAC-SHA256 Token issuance)"]
        DocProfSvc["DoctorProfileService"]
        PatProfSvc["PatientProfileService"]
        AvailSvc["DoctorAvailabilityService"]
        ApptSvc["AppointmentService (Concurrency & conflict prevention)"]
        ConsultSvc["ConsultationService"]
        AiSvc["ClinicalHistorySummaryService"]
    end

    subgraph External["External Integrations"]
        AiClient["OpenAiCompatibleClinicalSummaryClient"]
        OpenAiApi["OpenAI / LLM API Endpoint"]
    end

    subgraph Persistence["Data Access Tier (Spring Data JPA)"]
        Repos["JPA Repositories (User, DoctorProfile, Availability, Appt, Consultation)"]
        Postgres[("PostgreSQL Database (Port 5432)")]
    end

    UI --> ApiClient
    ApiClient --> Cors
    Cors --> JwtFilter
    JwtFilter --> SecCtx
    SecCtx --> Controllers
    Controllers --> Services
    Services --> Repos
    Repos --> Postgres
    AiSvc --> AiClient
    AiClient --> OpenAiApi
```

---

## 3. Database Entity Relationship Diagram (ERD)

```mermaid
erDiagram
    USER ||--o| DOCTOR_PROFILE : "has profile (if DOCTOR)"
    USER ||--o{ DOCTOR_AVAILABILITY : "doctor schedules"
    USER ||--o{ APPOINTMENT : "patient books"
    USER ||--o{ APPOINTMENT : "doctor attends"
    DOCTOR_AVAILABILITY ||--o{ APPOINTMENT : "allocated for"
    APPOINTMENT ||--o| CONSULTATION_RECORD : "yields documentation"
    CONSULTATION_RECORD ||--o{ PRESCRIPTION : "contains medications"

    USER {
        bigint id PK
        string public_id UK "Unique Public ID (PAT-/DOC-)"
        string full_name
        string email UK "Login identifier"
        string password_hash "BCrypt hash"
        string role "PATIENT or DOCTOR"
        string phone_number "Private patient phone"
        timestamp created_at
        timestamp updated_at
    }

    DOCTOR_PROFILE {
        bigint id PK
        bigint doctor_user_id FK, UK
        string specialty
        string clinic_name
        string clinic_address
        string city
        string area
        string pin_code
        string clinic_phone "Public clinic contact"
        string personal_phone "Private doctor phone"
        timestamp created_at
        timestamp updated_at
    }

    DOCTOR_AVAILABILITY {
        bigint id PK
        bigint doctor_id FK
        timestamp start_time
        timestamp end_time
        boolean is_active
        timestamp created_at
    }

    APPOINTMENT {
        bigint id PK
        bigint patient_id FK
        bigint doctor_id FK
        bigint availability_id FK
        timestamp start_time
        timestamp end_time
        string status "BOOKED, COMPLETED, CANCELLED"
        timestamp created_at
        timestamp updated_at
    }

    CONSULTATION_RECORD {
        bigint id PK
        bigint appointment_id FK, UK
        bigint doctor_id FK
        bigint patient_id FK
        text symptoms
        text diagnosis
        text treatment_advice
        date follow_up_date
        timestamp created_at
    }

    PRESCRIPTION {
        bigint id PK
        bigint consultation_record_id FK
        string medication_name
        string dosage
        string frequency
        string duration
    }
```

---

## 4. Security Architecture & Boundary Enforcement

### A. Authentication & Tokens
- **Stateless Model:** No HTTP sessions stored on server; every request contains `Authorization: Bearer <JWT>`.
- **Token Claims:** Contains `subject` (user email), `userId`, `publicId`, `role` (`ROLE_PATIENT` or `ROLE_DOCTOR`), and expiry timestamp.
- **Filter Order:** Custom `JwtAuthenticationFilter` executes before Spring Security's `UsernamePasswordAuthenticationFilter`.

### B. Public vs. Private Data Segregation
To prevent sensitive data leaks, MediGuide Care implements strict DTO boundary segregation:
1. **Public Doctor Search / Profile (`PublicDoctorProfileResponse`):**
   - Includes: `publicId`, `doctorName`, `specialty`, `clinicName`, `clinicAddress`, `city`, `area`, `pinCode`, `clinicPhone`.
   - Excludes: internal database IDs, login email, password hash, and `personalPhone`.
2. **Private Doctor Profile (`DoctorProfileResponse`):**
   - Accessible only by the authenticated doctor managing their own profile.
   - Includes `personalPhone` and `clinicPhone`.
3. **Private Patient Profile (`PatientProfileResponse`):**
   - Accessible only by the authenticated patient.
   - Encapsulates `publicId`, `fullName`, `email`, and `phoneNumber`.
   - Never exposed in doctor searches, public lookups, or unauthenticated endpoints.

---

## 5. Concurrency & Appointment Double-Booking Prevention

1. **Transactional Integrity:** Booking operations execute inside Spring's `@Transactional` boundary with isolation levels preventing phantom reads.
2. **Availability Verification:** The requested time interval must reside strictly within the doctor's active `DoctorAvailability` window.
3. **Conflict Detection:** The query verifies that no existing `BOOKED` appointment overlaps with the requested interval for either the doctor or the patient:
   ```sql
   WHERE doctor_id = :doctorId 
     AND status = 'BOOKED' 
     AND start_time < :newEndTime 
     AND end_time > :newStartTime
   ```
4. **Graceful Handling:** Conflicting requests trigger a clean `409 Conflict` response with an explanatory user message.
