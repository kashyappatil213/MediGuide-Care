import os
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

def set_cell_background(cell, hex_color):
    shading_elm = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{hex_color}"/>')
    cell._tc.get_or_add_tcPr().append(shading_elm)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement('w:tcMar')
    for m, val in [('w:top', top), ('w:bottom', bottom), ('w:left', left), ('w:right', right)]:
        node = OxmlElement(m)
        node.set(qn('w:w'), str(val))
        node.set(qn('w:type'), 'dxa')
        tcMar.append(node)
    tcPr.append(tcMar)

def create_document():
    doc = Document()

    # Page Margins
    for section in doc.sections:
        section.top_margin = Inches(0.8)
        section.bottom_margin = Inches(0.8)
        section.left_margin = Inches(0.9)
        section.right_margin = Inches(0.9)

    # Styles
    normal_style = doc.styles['Normal']
    normal_style.font.name = 'Calibri'
    normal_style.font.size = Pt(11)
    normal_style.font.color.rgb = RGBColor(30, 41, 59)

    # Colors
    PRIMARY = RGBColor(2, 132, 199)      # Medical Blue
    TEAL = RGBColor(13, 148, 136)        # Healthcare Teal
    DARK = RGBColor(15, 23, 42)          # Charcoal Text
    MUTED = RGBColor(71, 85, 105)        # Slate Grey

    # Document Title
    title_p = doc.add_paragraph()
    title_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title_p.paragraph_format.space_before = Pt(12)
    title_p.paragraph_format.space_after = Pt(4)
    run_title = title_p.add_run("MediGuide Care")
    run_title.font.size = Pt(28)
    run_title.font.bold = True
    run_title.font.color.rgb = PRIMARY

    # Subtitle
    sub_p = doc.add_paragraph()
    sub_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    sub_p.paragraph_format.space_after = Pt(18)
    run_sub = sub_p.add_run("Doctor–Patient Consultation & Clinic Management System")
    run_sub.font.size = Pt(14)
    run_sub.font.bold = True
    run_sub.font.color.rgb = TEAL

    # Metadata Block
    meta_p = doc.add_paragraph()
    meta_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    meta_p.paragraph_format.space_after = Pt(24)
    run_meta = meta_p.add_run("Comprehensive Technical & Architectural Project Documentation\nTechnology Stack: Java 17 | Spring Boot 3.2 | PostgreSQL 16 | React 19 | Vite | JWT Security")
    run_meta.font.size = Pt(9.5)
    run_meta.font.italic = True
    run_meta.font.color.rgb = MUTED

    doc.add_paragraph().paragraph_format.space_after = Pt(6)

    # Helper: Section Heading
    def add_heading_1(text):
        h = doc.add_paragraph()
        h.paragraph_format.space_before = Pt(18)
        h.paragraph_format.space_after = Pt(6)
        r = h.add_run(text)
        r.font.size = Pt(16)
        r.font.bold = True
        r.font.color.rgb = PRIMARY
        return h

    def add_heading_2(text):
        h = doc.add_paragraph()
        h.paragraph_format.space_before = Pt(12)
        h.paragraph_format.space_after = Pt(4)
        r = h.add_run(text)
        r.font.size = Pt(13)
        r.font.bold = True
        r.font.color.rgb = TEAL
        return h

    def add_bullet(text, bold_prefix=""):
        p = doc.add_paragraph(style='List Bullet')
        p.paragraph_format.space_after = Pt(3)
        if bold_prefix:
            r_bold = p.add_run(bold_prefix + ": ")
            r_bold.font.bold = True
            r_bold.font.color.rgb = DARK
        r_text = p.add_run(text)
        r_text.font.color.rgb = DARK
        return p

    # --- 1. Executive Summary ---
    add_heading_1("1. Executive Summary")
    p1 = doc.add_paragraph(
        "MediGuide Care is a fullstack clinic consultation and outpatient healthcare platform engineered to bridge "
        "the gap between patients and specialized medical doctors. It addresses traditional clinical hurdles such as "
        "chaotic phone appointment bookings, paper-based lost prescription notes, uncoordinated physician availability schedules, "
        "and inaccessible medical history."
    )
    p1.paragraph_format.space_after = Pt(6)

    p2 = doc.add_paragraph(
        "Built on an enterprise-grade Java 17 / Spring Boot backend and a modern React.js frontend, MediGuide Care provides "
        "robust role-based access control (Doctor and Patient), interactive calendar scheduling, specialized doctor discovery by "
        "locality and PIN code, digital prescription generation, and an OpenAI-compatible Clinical History Summarizer to empower "
        "physicians with instant longitudinal patient insights."
    )
    p2.paragraph_format.space_after = Pt(12)

    # --- 2. Key Architecture & Technology Stack ---
    add_heading_1("2. Technology Stack & System Architecture")

    table = doc.add_table(rows=7, cols=3)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    headers = ["Tier", "Technology / Framework", "Core Purpose & Responsibilities"]
    col_widths = [Inches(1.5), Inches(2.2), Inches(3.0)]

    hdr_cells = table.rows[0].cells
    for i, title in enumerate(headers):
        hdr_cells[i].text = title
        set_cell_background(hdr_cells[i], "0284C7")
        hdr_cells[i].paragraphs[0].runs[0].font.bold = True
        hdr_cells[i].paragraphs[0].runs[0].font.color.rgb = RGBColor(255, 255, 255)
        set_cell_margins(hdr_cells[i], 120, 120, 150, 150)

    rows_data = [
        ("Frontend Client", "React 19 + Vite + Modern CSS", "Single Page Application (SPA), zero bloat, mobile-responsive clean medical UI, tab-scoped token storage."),
        ("Backend Server", "Java 17 + Spring Boot 3.2", "RESTful API services, layered Controller-Service-Repository architecture, strict input validation."),
        ("Security Layer", "Spring Security 6 + JJWT + BCrypt", "Stateless JWT Bearer authentication, role-based method security (@PreAuthorize), CORS policies."),
        ("Database Layer", "PostgreSQL 16 + Hibernate / JPA", "Relational persistence with HikariCP pool, transactional integrity, foreign-key constraints."),
        ("API Docs", "SpringDoc OpenAPI 3 + Swagger UI", "Self-documenting interactive REST documentation available at /swagger-ui.html."),
        ("AI Clinical Client", "OpenAI-compatible HTTP Client", "Secure, non-diagnostic longitudinal consultation summarizer with clinician disclosure.")
    ]

    for row_idx, data in enumerate(rows_data):
        row_cells = table.rows[row_idx + 1].cells
        bg = "F8FAFC" if row_idx % 2 == 0 else "FFFFFF"
        for col_idx, text in enumerate(data):
            row_cells[col_idx].text = text
            set_cell_background(row_cells[col_idx], bg)
            set_cell_margins(row_cells[col_idx], 100, 100, 120, 120)
            if col_idx == 0:
                row_cells[col_idx].paragraphs[0].runs[0].font.bold = True

    for row in table.rows:
        for idx, width in enumerate(col_widths):
            row.cells[idx].width = width

    doc.add_paragraph().paragraph_format.space_after = Pt(12)

    # --- 3. Core Roles & Functional Workflows ---
    add_heading_1("3. Core Roles & Functional Workflows")

    add_heading_2("3.1 Patient Care Portal Workflows")
    add_bullet("Users choose between Patient or Doctor during registration. Upon signup, each patient is assigned a role-prefixed, cryptographically randomized public ID (e.g., PAT-104291) to shield database sequence numbers.", "Role Identity & Registration")
    add_bullet("Patients can search published doctors filtering by Medical Specialty (e.g., Cardiology, Pediatrics, Dermatology), City, Locality/Area, and 6-digit Indian PIN Code.", "Doctor & Clinic Discovery")
    add_bullet("Patients browse active, future consultation slots published by doctors. Selecting a slot automatically scrolls to booking with custom appointment interval selection.", "Availability Schedule Discovery")
    add_bullet("Real-time scheduling with concurrency checks prevents double-booking. Reference IDs are issued instantly.", "Instant Appointment Booking")
    add_bullet("Patients track all booked, completed, and cancelled appointments with status pills and doctor contact information.", "Booking Management & Cancellation")
    add_bullet("Patients can view full digital consultation records: diagnosis notes, clinician advice, and itemized digital prescriptions (medication, dosage, frequency, duration, usage instructions).", "Consultation & Prescription History")

    add_heading_2("3.2 Doctor Clinical Portal Workflows")
    add_bullet("Doctors define and publish their clinical practice details, specialty, clinic address, timings, contact phone, and consultation location for patient discovery.", "Clinic & Specialty Profile")
    add_bullet("Interactive calendar matrix allowing doctors to inspect daily schedules, publish single or batch availability slots (e.g., 09:00 - 13:00), and deactivate slots with 1-click.", "Calendar Availability Planner")
    add_bullet("Real-time triage queue of patients with booked appointments. Doctors can transition consultations to COMPLETED or CANCELLED.", "Consultation Queue & Status Lifecycle")
    add_bullet("Slide-over clinical drawer allowing the physician to enter structured clinical diagnoses, comprehensive treatment instructions, and itemized prescriptions.", "Clinical Documentation & Prescriptions")
    add_bullet("Doctors can select any patient from their history and request a structured AI clinical summary of previous diagnoses, treatments, and prescriptions to assist in clinical decision making (with clinician-in-the-loop disclosure).", "AI Clinical History Summarizer")

    # --- 4. Database Schema & Data Models ---
    add_heading_1("4. Relational Database Schema & Entities")
    doc.add_paragraph("The MediGuide Care relational schema is enforced via PostgreSQL foreign keys, unique constraints, and JPA indexes:")

    add_bullet("Stores credentials (email, hashed password, fullName, role: DOCTOR/PATIENT, emailVerified, publicId).", "users")
    add_bullet("One-to-One with User (DOCTOR). Specialty, clinicName, clinicAddress, city, area, pinCode, clinicPhone, personalPhone.", "doctor_profiles")
    add_bullet("One-to-One with User (PATIENT). Private patient contact number and preferences.", "patient_profiles")
    add_bullet("Many-to-One with User (DOCTOR). Defines future consultation time windows (startTime, endTime, isActive flag).", "doctor_availabilities")
    add_bullet("Links Patient, Doctor, and Availability. Tracks status: BOOKED, COMPLETED, CANCELLED.", "appointments")
    add_bullet("One-to-One with Appointment. Records clinical diagnosis, general advice, follow-up date, and timestamp.", "consultation_records")
    add_bullet("Many-to-One with ConsultationRecord. Medication name, dosage (e.g. 500mg), frequency (e.g. Twice daily), durationDays, and instructions (e.g. After meals).", "prescription_items")
    add_bullet("Single-use cryptographically random verification tokens with 24-hour expiration and 60-second cooldown.", "email_verification_tokens")

    # --- 5. Security Architecture ---
    add_heading_1("5. Security & Privacy Architecture")
    add_bullet("All passwords are encrypted using BCrypt with salt rounds, ensuring zero plaintext exposure.", "BCrypt Password Hashing")
    add_bullet("Tokens are signed with HMAC-SHA256 (minimum 256 bits). JWTs are held tab-scoped in memory, eliminating XSS vulnerability in localStorage.", "Stateless JWT Bearer Authentication")
    add_bullet("Internal database primary keys (IDs 1, 2, 3...) are masked externally behind public IDs (DOC-XXXXXX, PAT-XXXXXX) to thwart sequence harvesting.", "Public ID Obfuscation")
    add_bullet("Spring Security 6 filters validate JWT tokens and enforce role-based authorization (@PreAuthorize('hasRole(\"DOCTOR\")')).", "Role-Based Method Security")
    add_bullet("Patient phone numbers and detailed clinical records are restricted strictly to the consulting doctor and patient.", "HIPAA/Privacy Standards")

    # --- 6. Automated Testing & Verification ---
    add_heading_1("6. Quality Assurance & Test Verification")
    p_test = doc.add_paragraph(
        "The project includes comprehensive automated test suites covering repository operations, service business logic, "
        "concurrency locks, controller mappings, and security filters:"
    )
    p_test.paragraph_format.space_after = Pt(4)

    add_bullet("298 automated tests pass with 0 failures, 0 errors, and 0 skipped.", "Test Suite Coverage")
    add_bullet("Verified concurrent booking requests to ensure slots cannot be double-booked by parallel requests.", "Appointment Concurrency Tests")
    add_bullet("Tested token creation, claim extraction, tamper detection, and expiration handling.", "JWT Security Tests")
    add_bullet("Mocked OpenAI API responses, latency timeouts, and prompt formatting.", "AI Clinical Client Tests")

    # --- 7. Execution & Running Guide ---
    add_heading_1("7. Setup & Execution Instructions")
    doc.add_paragraph("To run MediGuide Care locally:")

    add_bullet("Ensure PostgreSQL is running on port 5432 with database `mediguide_db` created.", "Step 1: Database Setup")
    add_bullet("Navigate to `backend/` and execute `mvn spring-boot:run`. The backend will start on `http://localhost:8080`.", "Step 2: Start Spring Boot Backend")
    add_bullet("Navigate to `frontend/` and execute `npm run dev`. The React application will start on `http://localhost:5173`.", "Step 3: Start React Frontend")
    add_bullet("Visit `http://localhost:5173/` in any browser to access the clean clinical portal.", "Step 4: Access System")
    add_bullet("Swagger UI documentation is available at `http://localhost:8080/swagger-ui.html`.", "Step 5: API Documentation")

    # Save
    output_path = r"e:\Mini Project\MediGuide Care\MediGuide_Care_Project_Documentation.docx"
    doc.save(output_path)
    print(f"Successfully generated: {output_path}")

if __name__ == "__main__":
    create_document()
