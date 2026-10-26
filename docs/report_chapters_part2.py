"""
MediGuide Care — Academic Report Chapters 7 to 10 Content Builder
"""

import os
from reportlab.platypus import Paragraph, PageBreak, Spacer, Table, TableStyle, Image, KeepTogether
from reportlab.lib.units import inch
from report_base import (
    USABLE_WIDTH, COLOR_PRIMARY, COLOR_SECONDARY, TrackedBookmark, format_academic_table, create_code_box
)

DIAGRAMS_DIR = os.path.join(os.path.dirname(__file__), "diagrams")

def get_chapters_7_to_10_story(styles, tracker):
    story = []

    # ==========================================================================
    # CHAPTER 7: SYSTEM ARCHITECTURE AND WORKFLOW DIAGRAMS
    # ==========================================================================
    story.append(TrackedBookmark("ch7", tracker))
    story.append(Paragraph("<b>CHAPTER 7: SYSTEM ARCHITECTURE AND WORKFLOW DIAGRAMS</b>", styles['chapter']))
    story.append(Spacer(1, 10))

    story.append(TrackedBookmark("sec7_1", tracker))
    story.append(Paragraph("<b>7.1 Overall System Architecture</b>", styles['h1']))
    story.append(Paragraph(
        "MediGuide Care is architected as a decoupled, multi-tier distributed web application designed for high availability, "
        "strict data boundary enforcement, and maintainability. Figure 7.1 illustrates the structural interaction across all application layers, "
        "including the React 19 Single-Page Application (SPA) client, the Spring Security 6 gateway, the Spring Boot REST controllers, "
        "the core business service layer, the PostgreSQL relational persistence layer, and external integration points (SMTP and AI summarizer).",
        styles['body']
    ))

    # Figure 7.1 Embed
    story.append(TrackedBookmark("fig7_1", tracker))
    fig1_path = os.path.join(DIAGRAMS_DIR, "fig1_system_architecture.png")
    if os.path.exists(fig1_path):
        img1 = Image(fig1_path, width=USABLE_WIDTH, height=USABLE_WIDTH * 0.62)
        story.append(img1)
    story.append(Paragraph("<b>Figure 7.1:</b> Overall Multi-Tier System Architecture of MediGuide Care", styles['caption']))
    story.append(Spacer(1, 8))

    story.append(TrackedBookmark("sec7_2", tracker))
    story.append(Paragraph("<b>7.2 Registration, Login, JWT, and Email Verification Workflow</b>", styles['h1']))
    story.append(Paragraph(
        "User onboarding and authentication follow a multi-stage security lifecycle depicted in Figure 7.2. "
        "Upon registration, user passwords are salted and hashed using BCrypt. The system assigns a unique public identifier "
        "(<code>PAT-XXXXXX</code> for patients or <code>DOC-XXXXXX</code> for doctors) and persists the record with <code>email_verified = false</code>. "
        "Simultaneously, a 32-byte cryptographically secure random token is generated. Only its 64-character SHA-256 hash is stored in <code>email_verification_tokens</code>, "
        "while the plaintext token is transmitted in a verification hyperlink via SMTP. Upon clicking the link, the user's account is validated and the token is consumed. "
        "Stateless logins issue an HMAC-SHA256 signed JWT containing the user's email, role, public ID, and verification status, which the frontend stores in-memory.",
        styles['body']
    ))

    # Figure 7.2 Embed
    story.append(TrackedBookmark("fig7_2", tracker))
    fig2_path = os.path.join(DIAGRAMS_DIR, "fig2_auth_verification_workflow.png")
    if os.path.exists(fig2_path):
        img2 = Image(fig2_path, width=USABLE_WIDTH, height=USABLE_WIDTH * 0.62)
        story.append(img2)
    story.append(Paragraph("<b>Figure 7.2:</b> User Registration, Login, JWT Authentication, and Email Verification Lifecycle", styles['caption']))
    story.append(PageBreak())

    story.append(TrackedBookmark("sec7_3", tracker))
    story.append(Paragraph("<b>7.3 Doctor Availability and Patient Appointment Booking Workflow</b>", styles['h1']))
    story.append(Paragraph(
        "Figure 7.3 details the dual-lane workflow governing doctor schedule creation and patient appointment reservations. "
        "A verified doctor publishes availability intervals via the calendar scheduler. The backend validates that the start time precedes the end time, "
        "ensures the interval is in the future, and verifies that no overlapping availability window exists for that doctor. "
        "Prospective patients discover active slots through multi-field search. During booking, <code>AppointmentService</code> acquires "
        "exclusive pessimistic row-level write locks on both the patient and doctor records in ascending User ID order, executes a transactional "
        "SQL overlap range query, and atomically creates the appointment in <code>BOOKED</code> status, preventing concurrent double-booking.",
        styles['body']
    ))

    # Figure 7.3 Embed
    story.append(TrackedBookmark("fig7_3", tracker))
    fig3_path = os.path.join(DIAGRAMS_DIR, "fig3_availability_booking_workflow.png")
    if os.path.exists(fig3_path):
        img3 = Image(fig3_path, width=USABLE_WIDTH, height=USABLE_WIDTH * 0.62)
        story.append(img3)
    story.append(Paragraph("<b>Figure 7.3:</b> Doctor Availability Scheduling and Concurrency-Safe Appointment Booking Workflow", styles['caption']))
    story.append(Spacer(1, 8))

    story.append(TrackedBookmark("sec7_4", tracker))
    story.append(Paragraph("<b>7.4 Clinical Consultation, Prescription, and Follow-Up Workflow</b>", styles['h1']))
    story.append(Paragraph(
        "The consultation lifecycle shown in Figure 7.4 connects appointment scheduling with clinical documentation. "
        "From the attending queue, the doctor selects a patient in <code>BOOKED</code> status and opens the slide-over consultation drawer. "
        "The doctor inputs clinical observations, symptoms, medical diagnosis, treatment advice, and recommended follow-up date. "
        "Additionally, multiple digital prescription items (medication name, dosage, frequency, duration) can be attached. "
        "Submitting the drawer initiates an atomic database transaction that persists the <code>ConsultationRecord</code> and <code>Prescription</code> entities, "
        "and transitions the appointment status from <code>BOOKED</code> to <code>COMPLETED</code>. The record immediately becomes accessible "
        "on the patient's chronological care timeline and the doctor's historical consultation registry.",
        styles['body']
    ))

    # Figure 7.4 Embed
    story.append(TrackedBookmark("fig7_4", tracker))
    fig4_path = os.path.join(DIAGRAMS_DIR, "fig4_consultation_prescription_workflow.png")
    if os.path.exists(fig4_path):
        img4 = Image(fig4_path, width=USABLE_WIDTH, height=USABLE_WIDTH * 0.62)
        story.append(img4)
    story.append(Paragraph("<b>Figure 7.4:</b> Clinical Consultation Documentation, Digital Prescriptions, and Care Timeline Lifecycle", styles['caption']))
    story.append(PageBreak())

    story.append(TrackedBookmark("sec7_5", tracker))
    story.append(Paragraph("<b>7.5 AI-Assisted Clinical History Summarization Workflow and Safeguards</b>", styles['h1']))
    story.append(Paragraph(
        "Figure 7.5 illustrates the multi-tier safeguards governing the AI clinical history summarizer. "
        "When an attending doctor requests an AI summary for a patient, the frontend requires the doctor to confirm an explicit clinical disclosure acknowledgement. "
        "The backend verifies that the doctor is actively authorized to consult the patient, retrieves past consultations, strips all internal database keys and personal contact data, "
        "and bounds the payload within a strict character budget (max 6,000 characters). "
        "The payload is dispatched alongside a strict medical safety system prompt that forbids inferring new diagnoses or changing drug dosages. "
        "The returned response is explicitly labeled as an unverified draft for physician review, with copy-to-clipboard functionality enabling "
        "the doctor to verify and incorporate relevant excerpts into the consultation drawer.",
        styles['body']
    ))

    # Figure 7.5 Embed
    story.append(TrackedBookmark("fig7_5", tracker))
    fig5_path = os.path.join(DIAGRAMS_DIR, "fig5_ai_summary_workflow.png")
    if os.path.exists(fig5_path):
        img5 = Image(fig5_path, width=USABLE_WIDTH, height=USABLE_WIDTH * 0.62)
        story.append(img5)
    story.append(Paragraph("<b>Figure 7.5:</b> AI-Assisted Clinical History Summarizer Workflow and Multi-Layer Medical Safeguards", styles['caption']))
    story.append(PageBreak())

    # ==========================================================================
    # CHAPTER 8: DATABASE DESIGN, ENTITIES, RELATIONSHIPS, AND DATA FLOW
    # ==========================================================================
    story.append(TrackedBookmark("ch8", tracker))
    story.append(Paragraph("<b>CHAPTER 8: DATABASE DESIGN, ENTITIES, RELATIONSHIPS, AND DATA FLOW</b>", styles['chapter']))
    story.append(Spacer(1, 10))

    story.append(TrackedBookmark("sec8_1", tracker))
    story.append(Paragraph("<b>8.1 Relational Database Entity-Relationship Diagram (ERD)</b>", styles['h1']))
    story.append(Paragraph(
        "The persistence layer of MediGuide Care is modeled around seven relational tables in PostgreSQL 16. "
        "Figure 8.1 depicts the complete Entity-Relationship Diagram (ERD), showing primary keys (PK), foreign keys (FK), "
        "unique constraints (UK), and cardinalities across all domain entities.",
        styles['body']
    ))

    # Figure 8.1 Embed
    story.append(TrackedBookmark("fig8_1", tracker))
    fig6_path = os.path.join(DIAGRAMS_DIR, "fig6_entity_relationship_diagram.png")
    if os.path.exists(fig6_path):
        img6 = Image(fig6_path, width=USABLE_WIDTH, height=USABLE_WIDTH * 0.65)
        story.append(img6)
    story.append(Paragraph("<b>Figure 8.1:</b> Complete Relational Database Entity-Relationship Diagram (ERD)", styles['caption']))
    story.append(Spacer(1, 8))

    story.append(TrackedBookmark("sec8_2", tracker))
    story.append(Paragraph("<b>8.2 Entity Dictionary and Schema Definitions</b>", styles['h1']))
    story.append(Paragraph(
        "Table 8.1 provides the comprehensive data dictionary detailing column names, data types, constraints, and business descriptions "
        "for every table in the PostgreSQL database.",
        styles['body']
    ))

    # Table 8.1 Data Dictionary
    story.append(TrackedBookmark("tab8_1", tracker))
    dict_header = ["Table Name", "Column Name", "Data Type", "Constraints", "Description / Purpose"]
    dict_data = [
        ["users", "id", "BIGINT", "PK, AUTO_INC", "Internal surrogate primary key"],
        ["users", "public_id", "VARCHAR(30)", "UK, NOT NULL", "Public ID (PAT- / DOC-XXXXXX)"],
        ["users", "full_name", "VARCHAR(100)", "NOT NULL", "User full name for display"],
        ["users", "email", "VARCHAR(120)", "UK, NOT NULL", "Normalized login email address"],
        ["users", "password", "VARCHAR(255)", "NOT NULL", "Salted BCrypt password hash"],
        ["users", "role", "VARCHAR(20)", "NOT NULL", "Enum: PATIENT or DOCTOR"],
        ["users", "phone_number", "VARCHAR(30)", "NULLABLE", "Private patient contact telephone"],
        ["users", "email_verified", "BOOLEAN", "NOT NULL, DEF: TRUE", "Account email confirmation flag"],
        ["doctor_profiles", "id", "BIGINT", "PK, AUTO_INC", "Primary key for doctor profile"],
        ["doctor_profiles", "doctor_user_id", "BIGINT", "FK (users), UK", "One-to-one link to doctor user"],
        ["doctor_profiles", "specialty", "VARCHAR(100)", "NULLABLE", "Medical specialty (e.g. Cardiology)"],
        ["doctor_profiles", "clinic_name", "VARCHAR(150)", "NULLABLE", "Physical clinic/hospital name"],
        ["doctor_profiles", "city, area", "VARCHAR(100)", "NULLABLE", "Geographic municipality and locality"],
        ["doctor_profiles", "pin_code", "VARCHAR(10)", "NULLABLE", "Validated 6-digit Indian postal code"],
        ["doctor_profiles", "clinic_phone", "VARCHAR(30)", "NULLABLE", "Public clinic contact for inquiries"],
        ["doctor_profiles", "personal_phone", "VARCHAR(30)", "NULLABLE", "Strictly private doctor telephone"],
        ["doctor_availability", "id", "BIGINT", "PK, AUTO_INC", "Availability slot identifier"],
        ["doctor_availability", "doctor_id", "BIGINT", "FK (users)", "Many-to-one link to doctor"],
        ["doctor_availability", "start_time, end_time", "TIMESTAMP", "NOT NULL", "Consultation time interval bounds"],
        ["doctor_availability", "is_active", "BOOLEAN", "NOT NULL, DEF: TRUE", "Active discovery flag for patients"],
        ["appointments", "id", "BIGINT", "PK, AUTO_INC", "Appointment record identifier"],
        ["appointments", "patient_id", "BIGINT", "FK (users)", "Many-to-one link to booking patient"],
        ["appointments", "doctor_id", "BIGINT", "FK (users)", "Many-to-one link to attending doctor"],
        ["appointments", "availability_id", "BIGINT", "FK (availability)", "Slot within which booking falls"],
        ["appointments", "start_time, end_time", "TIMESTAMP", "NOT NULL", "Reserved consultation time bounds"],
        ["appointments", "status", "VARCHAR(20)", "NOT NULL", "Enum: BOOKED, COMPLETED, CANCELLED"],
        ["consultation_records", "id", "BIGINT", "PK, AUTO_INC", "Clinical consultation record key"],
        ["consultation_records", "appointment_id", "BIGINT", "FK (appts), UK", "One-to-one link to appointment"],
        ["consultation_records", "doctor_id, patient_id", "BIGINT", "FK (users)", "Attending physician and patient"],
        ["consultation_records", "symptoms, diagnosis", "TEXT", "NULLABLE", "Recorded clinical observations"],
        ["consultation_records", "treatment_advice", "TEXT", "NULLABLE", "Doctor's medical instructions"],
        ["consultation_records", "follow_up_date", "DATE", "NULLABLE", "Recommended next consultation date"],
        ["prescriptions", "id", "BIGINT", "PK, AUTO_INC", "Prescription line item key"],
        ["prescriptions", "consultation_record_id", "BIGINT", "FK (consultation)", "Parent consultation record link"],
        ["prescriptions", "medication_name", "VARCHAR(150)", "NOT NULL", "Prescribed drug / molecule name"],
        ["prescriptions", "dosage, frequency", "VARCHAR(100)", "NULLABLE", "Standardized dosage & timing"],
        ["prescriptions", "duration", "VARCHAR(50)", "NULLABLE", "Prescription treatment duration"],
        ["email_verification_tokens", "id", "BIGINT", "PK, AUTO_INC", "Verification token primary key"],
        ["email_verification_tokens", "user_id", "BIGINT", "FK (users), UK", "One-to-one link to user account"],
        ["email_verification_tokens", "token_hash", "VARCHAR(64)", "NOT NULL", "SHA-256 cryptographic token hash"],
        ["email_verification_tokens", "expires_at, last_sent_at", "TIMESTAMP", "NOT NULL", "Expiry & cooldown rate tracking"]
    ]
    dict_table = format_academic_table(dict_header, dict_data, [95, 80, 75, 85, 116], styles)
    story.append(dict_table)
    story.append(Paragraph("<b>Table 8.1:</b> Comprehensive Relational Database Data Dictionary", styles['caption']))
    story.append(Spacer(1, 10))

    story.append(TrackedBookmark("sec8_3", tracker))
    story.append(Paragraph("<b>8.3 Referential Integrity, Cascade Policies, and Constraints</b>", styles['h1']))
    story.append(Paragraph(
        "Foreign key constraints enforce referential integrity across all related tables. "
        "In <code>ConsultationRecord</code>, <code>appointment_id</code> carries a strict <code>UNIQUE</code> constraint, "
        "guaranteeing that an appointment can yield at most one clinical consultation record. "
        "A cascade policy (<code>CascadeType.ALL, orphanRemoval = true</code>) is applied from <code>ConsultationRecord</code> to <code>Prescription</code>, "
        "ensuring that all prescribed medications are persisted and cleaned up atomically alongside the parent record. "
        "Similarly, <code>EmailVerificationToken</code> has a <code>user_id UNIQUE</code> constraint, ensuring that at most one active verification token exists per account.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec8_4", tracker))
    story.append(Paragraph("<b>8.4 Data Flow and Transactional Boundaries</b>", styles['h1']))
    story.append(Paragraph(
        "Every write operation across the service layer is executed within an atomic Spring <code>@Transactional</code> boundary. "
        "In appointment booking, locking both user rows, verifying slot validity, and executing the conflict check occur within a single transaction; "
        "if an overlap is detected, the transaction rolls back cleanly with zero partial state committed. "
        "In consultation submission, persisting symptoms, diagnosis, prescription items, and updating the appointment status to <code>COMPLETED</code> "
        "occur atomically, guaranteeing complete consistency between appointments and consultation histories.",
        styles['body']
    ))
    story.append(PageBreak())

    # ==========================================================================
    # CHAPTER 9: MODULE-WISE IMPLEMENTATION
    # ==========================================================================
    story.append(TrackedBookmark("ch9", tracker))
    story.append(Paragraph("<b>CHAPTER 9: MODULE-WISE IMPLEMENTATION</b>", styles['chapter']))
    story.append(Spacer(1, 10))

    story.append(TrackedBookmark("sec9_1", tracker))
    story.append(Paragraph("<b>9.1 User Authentication, Role-Based Access Control, and Public IDs</b>", styles['h1']))
    story.append(Paragraph(
        "The authentication module (<code>AuthController</code>, <code>AuthService</code>) manages registration and login for patients and doctors. "
        "Self-registration as an administrator is strictly prohibited. Passwords are encrypted using BCrypt in <code>PasswordConfig</code>. "
        "To prevent sequential database enumeration, <code>PublicIdGenerator</code> assigns collision-resistant IDs: <code>DOC-XXXXXX</code> for doctors "
        "and <code>PAT-XXXXXX</code> for patients using <code>SecureRandom</code>. To ensure backward compatibility with legacy databases, "
        "<code>PublicIdBackfillRunner</code> automatically executes on startup to populate public IDs for any legacy accounts lacking one.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec9_2", tracker))
    story.append(Paragraph("<b>9.2 Doctor Profile Management & Location-Based Discovery Search</b>", styles['h1']))
    story.append(Paragraph(
        "Doctors configure their medical specialty, clinic name, physical address, municipality/city, area, 6-digit postal PIN code, "
        "clinic phone, and private personal phone via <code>DoctorProfileController</code>. "
        "The public search engine (<code>GET /api/v1/doctors</code>) allows patients to search by specialty, city, area, and PIN code. "
        "The underlying repository query utilizes case-insensitive substring matching for text fields and exact matching for PIN codes, "
        "combining active criteria using boolean AND logic. Search responses return <code>PublicDoctorProfileResponse</code>, which displays clinic contact details "
        "while strictly suppressing personal doctor phone numbers and login emails.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec9_3", tracker))
    story.append(Paragraph("<b>9.3 Availability Calendar Scheduler & Slot Management</b>", styles['h1']))
    story.append(Paragraph(
        "The availability module (<code>DoctorAvailabilityController</code>, <code>DoctorAvailabilityService</code>) empowers doctors to plan consultation hours. "
        "In the frontend, <code>DoctorAvailabilityCalendar</code> provides an intuitive visual month and day grid. "
        "Doctors can define multiple morning, afternoon, or evening slots. Backend validation ensures start times precede end times, "
        "slots reside in the future, and new intervals do not overlap existing active slots for that doctor. "
        "Doctors can deactivate unbooked availability slots, which immediately hides them from patient discovery.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec9_4", tracker))
    story.append(Paragraph("<b>9.4 Appointment Booking, Concurrency Prevention, and Lifecycle Management</b>", styles['h1']))
    story.append(Paragraph(
        "Managed by <code>AppointmentController</code> and <code>AppointmentService</code>, booking allows patients to reserve consultation intervals. "
        "Defensive validation ensures the patient's email is verified, the requested time falls within the doctor's active availability window, "
        "and start time precedes end time. Concurrency is handled via pessimistic write locking on user IDs in ascending order, followed by SQL range conflict checks. "
        "The appointment lifecycle supports status transitions from <code>BOOKED</code> to <code>COMPLETED</code> (upon consultation recording) "
        "or <code>CANCELLED</code> (prior to consultation by either party).",
        styles['body']
    ))

    story.append(TrackedBookmark("sec9_5", tracker))
    story.append(Paragraph("<b>9.5 Clinical Consultation Documentation & Digital Prescriptions Drawer</b>", styles['h1']))
    story.append(Paragraph(
        "When consulting a patient, the doctor opens the slide-over <code>DoctorClinicalDrawer</code>. "
        "The doctor records clinical observations, symptoms, diagnosis, treatment advice, and recommended follow-up date. "
        "Standardized digital prescription items (medication name, dosage, frequency, duration) are added dynamically. "
        "Submitting the consultation atomically stores the records and updates the appointment status to <code>COMPLETED</code>.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec9_6", tracker))
    story.append(Paragraph("<b>9.6 Patient Care Timeline & Historical Consultations</b>", styles['h1']))
    story.append(Paragraph(
        "Patients can inspect their complete medical history via <code>GET /api/v1/consultations/history/patient</code>. "
        "The timeline chronologically displays past consultations, diagnoses, prescribed medications, and follow-up guidance. "
        "Authorized doctors can also review historical consultation records for patients who have active or past appointments with that doctor, "
        "fostering informed clinical continuity.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec9_7", tracker))
    story.append(Paragraph("<b>9.7 AI-Assisted Clinical History Summarizer & Doctor-in-the-Loop Safeguards</b>", styles['h1']))
    story.append(Paragraph(
        "Implemented in <code>ClinicalHistorySummaryService</code>, the AI summarizer synthesizes patient consultation histories into structured review drafts. "
        "Medical safety is enforced at multiple levels: (1) doctors must explicitly confirm a disclosure acknowledgement; (2) prompts contain strictly chronological clinical data with personal identifiers stripped; "
        "(3) character limits guard against prompt injection; (4) system prompts prohibit inferring new diagnoses or changing dosages; "
        "and (5) output is presented strictly as a reviewable clinical draft with prominent safety disclaimers.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec9_8", tracker))
    story.append(Paragraph("<b>9.8 Email Verification System, Cooldown Rate Limits, and Anti-Enumeration</b>", styles['h1']))
    story.append(Paragraph(
        "Powered by <code>EmailVerificationService</code>, the email subsystem uses 32-byte Base64URL random tokens hashed with SHA-256 in <code>email_verification_tokens</code>. "
        "Tokens expire in 24 hours and are permanently deleted upon single use. A 60-second cooldown rate limit returns HTTP 429 with a <code>Retry-After</code> header if triggered. "
        "Uniform messaging for non-existent or verified emails prevents account enumeration attacks. Unverified accounts have progressive access: "
        "they can log in to view read-only dashboards, but high-trust actions (appointment booking and schedule publishing) require email verification.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec9_9", tracker))
    story.append(Paragraph("<b>9.9 Role-Based Access Control Matrix and Privacy Boundaries</b>", styles['h1']))
    story.append(Paragraph(
        "Figure 9.1 visualizes the complete Role-Based Access Control (RBAC) permission matrix, highlighting public access, "
        "patient capabilities, doctor privileges, and strict privacy boundaries.",
        styles['body']
    ))

    # Figure 9.1 Embed
    story.append(TrackedBookmark("fig9_1", tracker))
    fig7_path = os.path.join(DIAGRAMS_DIR, "fig7_rbac_workflow.png")
    if os.path.exists(fig7_path):
        img7 = Image(fig7_path, width=USABLE_WIDTH, height=USABLE_WIDTH * 0.62)
        story.append(img7)
    story.append(Paragraph("<b>Figure 9.1:</b> Role-Based Access Control (RBAC) Security Architecture and Privacy Boundaries", styles['caption']))
    story.append(PageBreak())

    # ==========================================================================
    # CHAPTER 10: API DESIGN, SECURITY, AND ENDPOINT DOCUMENTATION
    # ==========================================================================
    story.append(TrackedBookmark("ch10", tracker))
    story.append(Paragraph("<b>CHAPTER 10: API DESIGN, SECURITY, AND ENDPOINT DOCUMENTATION</b>", styles['chapter']))
    story.append(Spacer(1, 10))

    story.append(TrackedBookmark("sec10_1", tracker))
    story.append(Paragraph("<b>10.1 RESTful API Principles and Resource Mapping</b>", styles['h1']))
    story.append(Paragraph(
        "MediGuide Care's API is designed in strict compliance with REST architectural constraints. Resources are addressed using plural nouns "
        "(e.g., <code>/api/v1/doctors</code>, <code>/api/v1/appointments</code>, <code>/api/v1/consultations</code>). "
        "Standard HTTP verbs denote operations: <code>GET</code> for retrieval, <code>POST</code> for creation, and <code>PUT</code> for idempotent updates. "
        "All request payloads and responses use UTF-8 JSON. Every response includes standard HTTP status codes and structured response envelopes.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec10_2", tracker))
    story.append(Paragraph("<b>10.2 Comprehensive Endpoint Inventory</b>", styles['h1']))
    story.append(Paragraph(
        "Table 10.1 provides the complete inventory of all REST endpoints exposed by the Spring Boot backend.",
        styles['body']
    ))

    # Table 10.1 Endpoints
    story.append(TrackedBookmark("tab10_1", tracker))
    api_header = ["HTTP Method", "Endpoint Path", "Access Role", "Functional Description"]
    api_data = [
        ["POST", "/api/v1/auth/register", "Public", "Register patient or doctor account with BCrypt password"],
        ["POST", "/api/v1/auth/login", "Public", "Authenticate user and issue HMAC-SHA256 JWT access token"],
        ["POST", "/api/v1/auth/verify-email", "Public", "Validate token hash, verify user, delete token"],
        ["POST", "/api/v1/auth/resend-verification", "Public", "Rate-limited (60s cooldown) verification token resend"],
        ["GET", "/api/v1/doctors", "Public", "Search doctors by specialty, city, area, or PIN code"],
        ["GET", "/api/v1/doctors/{publicId}", "Public", "Public doctor profile details (clinic phone only)"],
        ["GET", "/api/v1/doctor/profile", "ROLE_DOCTOR", "Fetch authenticated doctor's full profile (with personal phone)"],
        ["PUT", "/api/v1/doctor/profile", "ROLE_DOCTOR", "Update doctor profile and clinic/personal phone numbers"],
        ["GET", "/api/v1/patient/profile", "ROLE_PATIENT", "Fetch authenticated patient's private contact profile"],
        ["PUT", "/api/v1/patient/profile", "ROLE_PATIENT", "Update authenticated patient's private phone number"],
        ["POST", "/api/v1/doctor/availability", "ROLE_DOCTOR", "Publish consultation availability slots (verified only)"],
        ["GET", "/api/v1/doctor/availability", "ROLE_DOCTOR", "List all availability slots scheduled by authenticated doctor"],
        ["DELETE", "/api/v1/doctor/availability/{id}", "ROLE_DOCTOR", "Deactivate unbooked doctor availability slot"],
        ["GET", "/api/v1/doctor/availability/active", "ROLE_PATIENT", "Browse active published slots for a given doctor"],
        ["POST", "/api/v1/appointments", "ROLE_PATIENT", "Book appointment with concurrency locks (verified only)"],
        ["GET", "/api/v1/appointments/patient", "ROLE_PATIENT", "List all consultation appointments booked by patient"],
        ["GET", "/api/v1/appointments/doctor", "ROLE_DOCTOR", "List all upcoming attending appointments for doctor"],
        ["PATCH", "/api/v1/appointments/{id}/cancel", "PATIENT/DOCTOR", "Cancel a booked consultation appointment"],
        ["POST", "/api/v1/consultations", "ROLE_DOCTOR", "Record consultation, diagnosis, Rx; mark COMPLETED"],
        ["GET", "/api/v1/consultations/history/patient", "ROLE_PATIENT", "Retrieve personal longitudinal care history timeline"],
        ["GET", "/api/v1/consultations/history/patient/{id}", "ROLE_DOCTOR", "Retrieve patient consultation history for attending doctor"],
        ["POST", "/api/v1/clinical-summary/patient/{id}", "ROLE_DOCTOR", "Generate AI clinical history summary with safety safeguards"],
        ["GET", "/api/v1/health", "Public", "System health check and database connectivity ping"]
    ]
    api_table = format_academic_table(api_header, api_data, [65, 145, 85, 156], styles)
    story.append(api_table)
    story.append(Paragraph("<b>Table 10.1:</b> Comprehensive MediGuide Care REST API Endpoint Inventory", styles['caption']))
    story.append(Spacer(1, 10))

    story.append(TrackedBookmark("sec10_3", tracker))
    story.append(Paragraph("<b>10.3 Authentication and Authorization Headers</b>", styles['h1']))
    story.append(Paragraph(
        "Protected endpoints require clients to transmit the JWT access token in the standard HTTP <code>Authorization</code> header: "
        "<code>Authorization: Bearer &lt;token&gt;</code>. The <code>JwtAuthenticationFilter</code> intercepts the request, verifies the signature "
        "against the server's 256-bit secret key, parses claims (subject, role, userId, publicId), and populates Spring's <code>SecurityContextHolder</code>. "
        "Unauthenticated requests to protected endpoints trigger <code>CustomAuthenticationEntryPoint</code> (HTTP 401 Unauthorized), "
        "while unauthorized role attempts trigger <code>CustomAccessDeniedHandler</code> (HTTP 403 Forbidden).",
        styles['body']
    ))

    story.append(TrackedBookmark("sec10_4", tracker))
    story.append(Paragraph("<b>10.4 Error Handling Schema and HTTP Status Code Semantics</b>", styles['h1']))
    story.append(Paragraph(
        "MediGuide Care handles exceptions uniformly through <code>GlobalExceptionHandler</code>, returning structured JSON error bodies "
        "containing <code>timestamp</code>, <code>status</code>, <code>error</code>, and <code>message</code>. Table 10.2 details the status code semantics.",
        styles['body']
    ))

    # Table 10.2 HTTP Status Codes
    story.append(TrackedBookmark("tab10_2", tracker))
    err_header = ["HTTP Status", "Error Classification", "Domain Trigger / Scenario", "Client Remediation"]
    err_data = [
        ["200 OK", "Success", "Resource retrieved or updated successfully", "Process response payload"],
        ["201 Created", "Resource Created", "User registered, appointment booked, consultation saved", "Record created successfully"],
        ["400 Bad Request", "Validation Error", "Invalid date format, end time before start time, past time", "Correct input fields and retry"],
        ["401 Unauthorized", "Authentication Required", "Missing, invalid, expired, or tampered JWT token", "Log in again to obtain fresh JWT"],
        ["403 Forbidden", "Access Denied", "Patient invoking doctor API or doctor viewing unassigned patient", "Verify permissions for caller role"],
        ["404 Not Found", "Resource Not Found", "Doctor, slot, or appointment ID does not exist", "Check identifier in URL request path"],
        ["409 Conflict", "Concurrency Conflict", "Appointment slot already booked or availability overlap", "Select a different time interval"],
        ["429 Too Many Requests", "Rate Limit Exceeded", "Resend verification requested within 60s cooldown", "Wait for Retry-After duration"]
    ]
    err_table = format_academic_table(err_header, err_data, [85, 95, 145, 126], styles)
    story.append(err_table)
    story.append(Paragraph("<b>Table 10.2:</b> HTTP Status Code Semantics and Error Mapping", styles['caption']))
    story.append(PageBreak())

    return story

print("Chapters 7 to 10 builder ready.")
