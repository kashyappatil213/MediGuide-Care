"""
MediGuide Care — Academic Report Chapters 1 to 6 Content Builder
"""

from reportlab.platypus import Paragraph, PageBreak, Spacer, Table, TableStyle, KeepTogether
from reportlab.lib.units import inch
from report_base import (
    USABLE_WIDTH, COLOR_PRIMARY, COLOR_SECONDARY, TrackedBookmark, format_academic_table, create_code_box
)

def get_chapters_1_to_6_story(styles, tracker):
    story = []

    # ==========================================================================
    # CHAPTER 1: INTRODUCTION
    # ==========================================================================
    story.append(TrackedBookmark("ch1", tracker))
    story.append(Paragraph("<b>CHAPTER 1: INTRODUCTION</b>", styles['chapter']))
    story.append(Spacer(1, 10))

    story.append(TrackedBookmark("sec1_1", tracker))
    story.append(Paragraph("<b>1.1 Project Overview and Domain Context</b>", styles['h1']))
    story.append(Paragraph(
        "Modern outpatient healthcare delivery relies heavily on efficient communication, precise appointment scheduling, "
        "and accurate record-keeping between clinical practitioners and patients. In many small-to-medium clinical settings and private outpatient practices, "
        "operations continue to depend on fragmented manual processes, physical ledger books, unstructured telephone or messaging exchanges, and physical paper prescriptions. "
        "These legacy practices introduce severe operational bottlenecks, including scheduling collisions, illegible handwritten treatment instructions, "
        "scattered clinical consultation histories, and accidental exposure of private contact details.",
        styles['body']
    ))
    story.append(Paragraph(
        "<b>MediGuide Care</b> is conceived and developed as an integrated Doctor–Patient Consultation and Follow-up Management System. "
        "It provides a secure, multi-tier web application designed to transition outpatient clinics into a streamlined digital operational model. "
        "The platform unifies patient doctor discovery, dynamic calendar-based availability planning, concurrency-safe appointment booking, "
        "slide-over clinical consultation documentation, structured digital prescriptions, and automated AI-assisted clinical history summarization into an accessible, cohesive interface.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec1_2", tracker))
    story.append(Paragraph("<b>1.2 Purpose and Objectives</b>", styles['h1']))
    story.append(Paragraph(
        "The primary purpose of MediGuide Care is to eliminate administrative overhead, prevent scheduling conflicts, "
        "and improve the quality and continuity of clinical consultations. The core objectives include:",
        styles['body']
    ))
    story.append(Paragraph("• <b>Streamlined Scheduling:</b> Provide physicians with a calendar-based interface to publish distinct availability intervals and allow verified patients to schedule consultation slots with zero double-booking.", styles['bullet']))
    story.append(Paragraph("• <b>Structured Clinical Documentation:</b> Enable doctors to document symptoms, formal diagnoses, treatment advice, and multi-item digital prescriptions with standardized dosage and frequency attributes.", styles['bullet']))
    story.append(Paragraph("• <b>Rigorous Data Security and Privacy:</b> Implement stateless token-based authentication, cryptographic password hashing, and strict role-based access control, while strictly segregating public clinic details from private personal telephone numbers.", styles['bullet']))
    story.append(Paragraph("• <b>AI-Assisted History Synthesis:</b> Equip attending doctors with automated, chronological summaries of prior patient consultations, structured with explicit medical safety safeguards and reviewable draft disclaimers.", styles['bullet']))
    story.append(Paragraph("• <b>Cryptographic Email Verification:</b> Enforce account authenticity through SHA-256 hashed verification tokens, single-use consumption, anti-enumeration responses, and rate-limiting cooldown protection.", styles['bullet']))

    story.append(TrackedBookmark("sec1_3", tracker))
    story.append(Paragraph("<b>1.3 Target Users and Operating Context</b>", styles['h1']))
    story.append(Paragraph(
        "MediGuide Care is tailored to serve two primary user classifications within outpatient clinical ecosystems:",
        styles['body']
    ))
    story.append(Paragraph(
        "1. <b>Patients:</b> Individuals seeking medical consultation who need to search for qualified practitioners by medical specialty and geographic locality (city, area, and postal PIN code), "
        "reserve convenient consultation times, review their complete historical treatment timeline, and safeguard their private telephone numbers.",
        styles['body']
    ))
    story.append(Paragraph(
        "2. <b>Consulting Doctors:</b> Licensed medical practitioners who require tools to publish flexible consultation schedules across multi-slot calendars, "
        "inspect attending queues, document structured electronic prescriptions, and review previous patient diagnoses via on-demand clinical summaries.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec1_4", tracker))
    story.append(Paragraph("<b>1.4 Organization of the Report</b>", styles['h1']))
    story.append(Paragraph(
        "This project report is organized into fourteen comprehensive chapters. Chapter 2 details the problem statement, scope, and motivation. "
        "Chapter 3 compares existing manual workflows against the proposed system. Chapter 4 specifies hardware and software requirements. "
        "Chapter 5 justifies the chosen technology stack. Chapter 6 covers system analysis, design patterns, and concurrency mechanics. "
        "Chapter 7 illustrates architectural workflows through high-resolution diagrams. Chapter 8 provides the complete database design and entity dictionary. "
        "Chapter 9 details module-by-module implementation. Chapter 10 documents API contracts and security rules. "
        "Chapter 11 presents the automated testing strategy and verified test results (298 passing tests). "
        "Chapter 12 analyzes the user interface design. Chapter 13 reviews limitations and future enhancements, and Chapter 14 concludes the report.",
        styles['body']
    ))
    story.append(PageBreak())

    # ==========================================================================
    # CHAPTER 2: PROBLEM STATEMENT, OBJECTIVES, SCOPE, AND MOTIVATION
    # ==========================================================================
    story.append(TrackedBookmark("ch2", tracker))
    story.append(Paragraph("<b>CHAPTER 2: PROBLEM STATEMENT, OBJECTIVES, SCOPE, AND MOTIVATION</b>", styles['chapter']))
    story.append(Spacer(1, 10))

    story.append(TrackedBookmark("sec2_1", tracker))
    story.append(Paragraph("<b>2.1 Problem Statement and Real-World Clinic Challenges</b>", styles['h1']))
    story.append(Paragraph(
        "Outpatient healthcare practices face critical operational challenges stemming from reliance on paper records and ad-hoc communication channels:",
        styles['body']
    ))
    story.append(Paragraph("• <b>Scheduling Conflicts & Double-Booking:</b> Manual appointment books cannot coordinate real-time reservations. When patients book through phone calls or front-desk clerks simultaneously, race conditions result in overlapping slots, extended patient wait times, and physician fatigue.", styles['bullet']))
    story.append(Paragraph("• <b>Loss of Longitudinal Medical History:</b> Paper case sheets and physical prescription slips are easily lost, damaged, or misplaced by patients. When consulting a doctor, past medical histories, drug allergies, and earlier treatment responses are frequently unavailable.", styles['bullet']))
    story.append(Paragraph("• <b>Unstructured Handwritten Prescriptions:</b> Illegible handwriting on prescription pads poses grave clinical risks, including medication dispensing errors, incorrect dosage administration, and ambiguous intake frequency.", styles['bullet']))
    story.append(Paragraph("• <b>Privacy Violations & Contact Exposure:</b> In conventional communication (e.g. WhatsApp or personal phone calls), doctor personal telephone numbers and patient private contact details are exposed, causing boundary violations and spam vulnerabilities.", styles['bullet']))
    story.append(Paragraph("• <b>Unverified Bot & Phantom Bookings:</b> Open booking forms without rigorous email verification permit fictitious account creation and abandoned appointment reservations, depriving genuine patients of consultation opportunities.", styles['bullet']))

    story.append(TrackedBookmark("sec2_2", tracker))
    story.append(Paragraph("<b>2.2 Project Objectives</b>", styles['h1']))
    story.append(Paragraph(
        "To decisively overcome these vulnerabilities, MediGuide Care establishes the following engineering goals:",
        styles['body']
    ))
    story.append(Paragraph("1. Engineer a stateless, secure multi-tier RESTful application adhering to the Controller-Service-Repository architectural pattern.", styles['bullet']))
    story.append(Paragraph("2. Eliminate sequential enumeration attacks by replacing internal database IDs with collision-resistant public alphanumeric identifiers (<code>PAT-XXXXXX</code> and <code>DOC-XXXXXX</code>).", styles['bullet']))
    story.append(Paragraph("3. Guarantee zero double-booking under concurrent traffic using database-level pessimistic write locking on ascending user IDs combined with transactional SQL overlap validation.", styles['bullet']))
    story.append(Paragraph("4. Deliver a responsive single-page web client providing distinct, role-enforced portals for patients and doctors.", styles['bullet']))
    story.append(Paragraph("5. Provide structured electronic prescriptions and digital consultation records automatically bound to appointment completion.", styles['bullet']))
    story.append(Paragraph("6. Integrate an automated, sanitized AI clinical history summarizer with doctor-in-the-loop clinical disclosure safeguards.", styles['bullet']))
    story.append(Paragraph("7. Implement cryptographic, rate-limited email verification to protect high-trust clinic operations.", styles['bullet']))

    story.append(TrackedBookmark("sec2_3", tracker))
    story.append(Paragraph("<b>2.3 Project Scope</b>", styles['h1']))
    story.append(Paragraph(
        "<b>In-Scope Capabilities (Implemented and Verified):</b>",
        styles['body']
    ))
    story.append(Paragraph("• Patient and doctor self-registration with salted BCrypt password hashing.", styles['bullet']))
    story.append(Paragraph("• Stateless HMAC-SHA256 JWT access token generation, validation, and in-memory client storage.", styles['bullet']))
    story.append(Paragraph("• Cryptographic email verification using 32-byte Base64URL tokens hashed with SHA-256 and 60-second cooldown rate-limiting.", styles['bullet']))
    story.append(Paragraph("• Multi-field doctor discovery search filtering by specialty, city, locality/area, and 6-digit Indian PIN code.", styles['bullet']))
    story.append(Paragraph("• Doctor calendar availability planning with multi-slot creation, interval overlap validation, and soft deactivation.", styles['bullet']))
    story.append(Paragraph("• Pessimistic row-locked appointment booking preventing double-booking race conditions.", styles['bullet']))
    story.append(Paragraph("• Full appointment lifecycle state machine: <code>BOOKED</code> → <code>COMPLETED</code> or <code>CANCELLED</code>.", styles['bullet']))
    story.append(Paragraph("• Slide-over clinical consultation drawer for documenting symptoms, diagnoses, advice, follow-up dates, and multi-item prescriptions.", styles['bullet']))
    story.append(Paragraph("• Patient chronological care timeline and authorized doctor consultation history lookup.", styles['bullet']))
    story.append(Paragraph("• OpenAI-compatible AI clinical history summarizer with data anonymization and safety prompt safeguards.", styles['bullet']))
    story.append(Paragraph("• Strict data isolation: patient private phone numbers and doctor personal phone numbers are inaccessible via public endpoints.", styles['bullet']))
    story.append(Paragraph("• Automated test suite with 298 passing unit, integration, repository, and concurrency tests.", styles['bullet']))
    story.append(Spacer(1, 4))
    story.append(Paragraph(
        "<b>Explicitly Out-of-Scope Roadmap (Planned for Subsequent Sprints):</b>",
        styles['body']
    ))
    story.append(Paragraph("• Asynchronous distributed message brokers (RabbitMQ / Kafka) and dead-letter retry queues.", styles['bullet']))
    story.append(Paragraph("• Real-time video/audio telemedicine consultations (WebRTC).", styles['bullet']))
    story.append(Paragraph("• SMS gateway delivery and QR-code summary sharing for mobile devices.", styles['bullet']))
    story.append(Paragraph("• Container orchestration (Docker Compose / Kubernetes) and continuous deployment pipelines (Jenkins).", styles['bullet']))

    story.append(TrackedBookmark("sec2_4", tracker))
    story.append(Paragraph("<b>2.4 Academic and Practical Motivation</b>", styles['h1']))
    story.append(Paragraph(
        "Developing MediGuide Care serves as a capstone application of enterprise software engineering principles for the Master of Computer Applications (MCA) curriculum. "
        "It provides hands-on mastery of enterprise Java with Spring Boot, modern reactive component architectures with React 19, relational ACID transaction management with PostgreSQL and Hibernate, "
        "cryptographic security with JWT and salted hashing, and defensive concurrency engineering under multi-threaded database transactions. "
        "Practically, the project addresses real outpatient operational inefficiencies, demonstrating how modern web technologies can elevate healthcare delivery.",
        styles['body']
    ))
    story.append(PageBreak())

    # ==========================================================================
    # CHAPTER 3: EXISTING SYSTEM AND PROPOSED SYSTEM
    # ==========================================================================
    story.append(TrackedBookmark("ch3", tracker))
    story.append(Paragraph("<b>CHAPTER 3: EXISTING SYSTEM AND PROPOSED SYSTEM</b>", styles['chapter']))
    story.append(Spacer(1, 10))

    story.append(TrackedBookmark("sec3_1", tracker))
    story.append(Paragraph("<b>3.1 Overview of Existing Outpatient Workflows</b>", styles['h1']))
    story.append(Paragraph(
        "In traditional clinic consultation workflows, scheduling is managed either via physical paper appointment registers "
        "or through informal digital channels such as phone calls and direct messaging. When a patient arrives, clinic receptionists manually record patient details, "
        "retrieve physical paper folders, and direct patients to waiting areas without transparent queue timing. "
        "During consultations, physicians handwrite clinical findings, diagnoses, and medication prescriptions on paper slips. "
        "Patients retain these slips, while clinics store brief handwritten ledger entries. "
        "Follow-up scheduling is typically handled verbally, with patients instructed to return after a specified number of days.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec3_2", tracker))
    story.append(Paragraph("<b>3.2 Drawbacks and Security Vulnerabilities of Current Systems</b>", styles['h1']))
    story.append(Paragraph(
        "Conventional clinic consultation models suffer from fundamental architectural and operational vulnerabilities:",
        styles['body']
    ))
    story.append(Paragraph("• <b>High Vulnerability to Race Conditions:</b> Simultaneous bookings taken via phone and in-person front desks frequently collide, resulting in embarrassing double-bookings and prolonged patient disputes.", styles['bullet']))
    story.append(Paragraph("• <b>Zero Data Integrity and Loss:</b> Physical prescription pads and case records are subject to physical wear, water damage, fire risk, and misplacement, permanently severing historical medical continuity.", styles['bullet']))
    story.append(Paragraph("• <b>Severe Prescription Ambiguity:</b> Illegible medical handwriting contributes significantly to pharmacological errors in pharmacies, with patients receiving incorrect drug molecules or dosages.", styles['bullet']))
    story.append(Paragraph("• <b>Absence of Role-Based Privacy:</b> When clinics share doctor phone numbers openly or store patient contact numbers in public spreadsheets, personal contact numbers are exposed to unauthorized marketing or harassment.", styles['bullet']))
    story.append(Paragraph("• <b>No Longitudinal Synthesis:</b> When examining a patient with recurring complaints, physicians have no automated mechanism to review historical diagnoses, forcing repetitive oral questioning during limited consultation windows.", styles['bullet']))

    story.append(TrackedBookmark("sec3_3", tracker))
    story.append(Paragraph("<b>3.3 Proposed MediGuide Care System</b>", styles['h1']))
    story.append(Paragraph(
        "MediGuide Care replaces manual and ad-hoc practices with an integrated, secure, multi-tier digital architecture. "
        "Physicians establish defined consultation availability intervals through an intuitive month/day visual calendar planner. "
        "Verified patients search for certified physicians across medical specialties and local geographies, browse real-time available slots, "
        "and execute guaranteed conflict-free reservations coordinated through database pessimistic row-level locking. "
        "Attending physicians document structured observations and digital multi-item prescriptions via a slide-over consultation drawer, "
        "which automatically marks the appointment as completed and publishes the consultation record to the patient's care timeline. "
        "Furthermore, physicians can invoke an AI clinical summarizer that sanitizes past records and synthesizes a structured review draft with doctor-in-the-loop safeguards.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec3_4", tracker))
    story.append(Paragraph("<b>3.4 Comparative Analysis Matrix</b>", styles['h1']))
    story.append(Paragraph(
        "Table 3.1 highlights the direct operational and architectural differences between conventional clinic systems and MediGuide Care.",
        styles['body']
    ))

    story.append(TrackedBookmark("tab3_1", tracker))
    comp_header = ["Dimension / Feature", "Existing Conventional System", "Proposed MediGuide Care System"]
    comp_data = [
        ["Scheduling Method", "Physical paper ledgers or unstructured phone calls", "Calendar-based multi-slot planner with real-time UI"],
        ["Concurrency Defense", "None; frequent overlapping double-bookings", "Pessimistic row-locking on ascending user IDs + range checks"],
        ["Prescription Delivery", "Handwritten paper slips; illegible handwriting risks", "Structured digital prescriptions with dosage, frequency, duration"],
        ["Medical History Access", "Scattered paper files; frequently lost by patients", "Permanent chronological care timeline accessible via portal"],
        ["User Identification", "Plain sequential integers; vulnerable to enumeration", "Obfuscated public alphanumeric IDs (DOC- / PAT-XXXXXX)"],
        ["Authentication & Auth", "Unsecured spreadsheets or basic session passwords", "Stateless HMAC-SHA256 JWT + salted BCrypt hashing + RBAC"],
        ["Account Verification", "Unverified phone numbers or open web forms", "Cryptographic SHA-256 tokens + 60s cooldown rate limiting"],
        ["Clinical History Review", "Manual page-turning of thick physical folders", "AI-assisted structured draft summary with safety safeguards"],
        ["Contact Privacy", "Uncontrolled disclosure of private phone numbers", "Strict segregation: public clinic phone vs. private personal phone"]
    ]
    comp_table = format_academic_table(comp_header, comp_data, [105, 170, 176], styles)
    story.append(comp_table)
    story.append(Paragraph("<b>Table 3.1:</b> Comparative Analysis Between Conventional Systems and MediGuide Care", styles['caption']))
    story.append(PageBreak())

    # ==========================================================================
    # CHAPTER 4: SOFTWARE AND HARDWARE REQUIREMENTS
    # ==========================================================================
    story.append(TrackedBookmark("ch4", tracker))
    story.append(Paragraph("<b>CHAPTER 4: SOFTWARE AND HARDWARE REQUIREMENTS</b>", styles['chapter']))
    story.append(Spacer(1, 10))

    story.append(TrackedBookmark("sec4_1", tracker))
    story.append(Paragraph("<b>4.1 Hardware Requirements</b>", styles['h1']))
    story.append(Paragraph(
        "To ensure reliable execution, compilation, and concurrent transaction management, the development, server, "
        "and client environments must satisfy the minimum specifications outlined in Table 4.1.",
        styles['body']
    ))

    story.append(TrackedBookmark("tab4_1", tracker))
    hw_header = ["Environment", "Component", "Minimum Requirement", "Recommended Specification"]
    hw_data = [
        ["Developer Workstation", "Processor", "Dual-Core x86_64 / ARM (2.0 GHz)", "Quad-Core Intel i5/i7 or AMD Ryzen 5/7 (3.0+ GHz)"],
        ["Developer Workstation", "RAM", "8 GB DDR4", "16 GB DDR4/DDR5"],
        ["Developer Workstation", "Storage", "20 GB available SSD space", "50 GB NVMe M.2 Solid State Drive"],
        ["Backend Server Host", "Processor", "1 vCPU (2.4 GHz)", "2+ vCPUs dedicated"],
        ["Backend Server Host", "RAM", "2 GB RAM (JVM heap: 1024 MB)", "4 GB+ RAM (JVM heap: 2048 MB)"],
        ["Backend Server Host", "Network", "10 Mbps continuous bandwidth", "100 Mbps / 1 Gbps low-latency interface"],
        ["Database Server", "RAM & Disk", "2 GB RAM, 10 GB SSD for tables/WAL", "4 GB RAM, High-IOPS NVMe SSD storage"],
        ["Client Devices", "Processor & RAM", "1.5 GHz Dual-Core, 2 GB RAM", "2.0 GHz Quad-Core, 4 GB+ RAM (Desktop/Tablet)"]
    ]
    hw_table = format_academic_table(hw_header, hw_data, [95, 80, 135, 141], styles)
    story.append(hw_table)
    story.append(Paragraph("<b>Table 4.1:</b> Hardware Requirements for MediGuide Care", styles['caption']))
    story.append(Spacer(1, 10))

    story.append(TrackedBookmark("sec4_2", tracker))
    story.append(Paragraph("<b>4.2 Software Requirements and Operating Environments</b>", styles['h1']))
    story.append(Paragraph(
        "The software components, runtime virtual machines, and development tools required for the project are summarized in Table 4.2.",
        styles['body']
    ))

    story.append(TrackedBookmark("tab4_2", tracker))
    sw_header = ["Category", "Software / Framework", "Version Tested", "Role / Operational Purpose"]
    sw_data = [
        ["Operating System", "Windows 10/11 / Ubuntu Linux", "Windows 11 (64-bit)", "Workstation and host operating platform"],
        ["JDK Runtime", "Eclipse Temurin OpenJDK / Oracle JDK", "Java SE 17.0.7 (LTS)", "Java Virtual Machine compiler and runtime"],
        ["Build Tool (Backend)", "Apache Maven", "Maven 3.8.x / 3.9.x", "Dependency resolution, compilation, test runner"],
        ["Relational Database", "PostgreSQL Server", "PostgreSQL 16.x", "Production ACID relational data storage"],
        ["In-Memory Database", "H2 Database Engine", "H2 2.2.x (test-scope)", "Isolated automated unit & integration testing"],
        ["Node Runtime (UI)", "Node.js & npm package manager", "Node 18+ (npm 9+)", "Frontend development server and bundle compiler"],
        ["Frontend Bundler", "Vite Build Tool", "Vite 8.3.4", "Fast Hot Module Replacement (HMR) and bundling"],
        ["Source Code Editor", "Visual Studio Code / IntelliJ IDEA", "VS Code & IntelliJ 2024", "Development, debugging, and git integration"]
    ]
    sw_table = format_academic_table(sw_header, sw_data, [90, 115, 95, 151], styles)
    story.append(sw_table)
    story.append(Paragraph("<b>Table 4.2:</b> Software Environment Specifications", styles['caption']))
    story.append(Spacer(1, 10))

    story.append(TrackedBookmark("sec4_3", tracker))
    story.append(Paragraph("<b>4.3 Runtime Dependencies and Library Specifications</b>", styles['h1']))
    story.append(Paragraph(
        "Key third-party dependencies defined in the Maven <code>pom.xml</code> and Node <code>package.json</code> include:",
        styles['body']
    ))

    story.append(TrackedBookmark("tab4_3", tracker))
    dep_header = ["Library / Dependency", "Group / Scope", "Artifact Version", "Functional Responsibility"]
    dep_data = [
        ["spring-boot-starter-web", "org.springframework.boot", "3.2.5", "Embedded Tomcat, DispatcherServlet, REST APIs"],
        ["spring-boot-starter-data-jpa", "org.springframework.boot", "3.2.5", "Spring Data JPA, Hibernate 6 ORM, HikariCP"],
        ["postgresql", "org.postgresql (runtime)", "42.6.2", "PostgreSQL JDBC Type-4 connection driver"],
        ["spring-boot-starter-security", "org.springframework.boot", "3.2.5", "SecurityFilterChain, authentication, filters"],
        ["jjwt-api / impl / jackson", "io.jsonwebtoken", "0.12.5", "HMAC-SHA256 JWT token encoding, parsing, claims"],
        ["spring-boot-starter-mail", "org.springframework.boot", "3.2.5", "JavaMailSender, Jakarta Mail SMTP transport"],
        ["springdoc-openapi-starter", "org.springdoc", "2.5.0", "OpenAPI v3 schema generator and Swagger UI"],
        ["spring-boot-starter-validation", "org.springframework.boot", "3.2.5", "Hibernate Validator runtime (@NotBlank, @Email)"],
        ["react & react-dom", "npm dependency", "19.0.0", "Declarative UI rendering and modular state hooks"],
        ["oxlint", "npm devDependency", "Latest", "High-performance JavaScript static code linter"]
    ]
    dep_table = format_academic_table(dep_header, dep_data, [110, 105, 75, 161], styles)
    story.append(dep_table)
    story.append(Paragraph("<b>Table 4.3:</b> Core Application Libraries and Dependencies", styles['caption']))
    story.append(PageBreak())

    # ==========================================================================
    # CHAPTER 5: TECHNOLOGY STACK AND ARCHITECTURAL JUSTIFICATION
    # ==========================================================================
    story.append(TrackedBookmark("ch5", tracker))
    story.append(Paragraph("<b>CHAPTER 5: TECHNOLOGY STACK AND ARCHITECTURAL JUSTIFICATION</b>", styles['chapter']))
    story.append(Spacer(1, 10))

    story.append(TrackedBookmark("sec5_1", tracker))
    story.append(Paragraph("<b>5.1 Backend: Java 17 LTS and Spring Boot 3.2.5</b>", styles['h1']))
    story.append(Paragraph(
        "Java 17 was selected as the foundational runtime because it is a Long-Term Support (LTS) release offering high stability, "
        "strong performance enhancements, and modern language features such as records, text blocks, pattern matching for switch, "
        "and sealed classes. <b>Spring Boot 3.2.5</b> leverages the Jakarta EE 10 baseline, delivering an production-ready application framework. "
        "It eliminates boilerplate XML configuration, embeds a high-throughput Apache Tomcat web container, and provides dependency injection "
        "enabling strict decoupling between controller, service, and repository tiers.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec5_2", tracker))
    story.append(Paragraph("<b>5.2 Persistence: PostgreSQL 16 and Hibernate 6 ORM</b>", styles['h1']))
    story.append(Paragraph(
        "PostgreSQL 16 was selected for persistence due to its enterprise-grade ACID compliance, robust indexing capabilities, "
        "and advanced transactional isolation levels. In healthcare scheduling, race conditions must be coordinated deterministically. "
        "PostgreSQL natively supports row-level exclusive write locks (<code>SELECT ... FOR UPDATE</code>), which MediGuide Care employs "
        "to prevent double-booking. <b>Spring Data JPA</b> backed by <b>Hibernate 6</b> maps relational tables to domain entities cleanly, "
        "manages foreign key constraints, and utilizes HikariCP for high-performance database connection pooling.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec5_3", tracker))
    story.append(Paragraph("<b>5.3 Security: Spring Security 6, BCrypt, and JJWT</b>", styles['h1']))
    story.append(Paragraph(
        "Authentication is architected as completely stateless using <b>Spring Security 6</b> and <b>JJWT 0.12.5</b>. "
        "Client requests transmit a compact, cryptographically signed Bearer JWT token in the HTTP <code>Authorization</code> header. "
        "Tokens are deterministically signed using HMAC-SHA256 with a high-entropy secret key of at least 256 bits. "
        "User passwords are never stored in plaintext; instead, they are hashed using the <b>BCrypt</b> salted key-derivation function "
        "configured in <code>PasswordConfig</code>. Fine-grained method security (<code>@PreAuthorize</code>) ensures that patients cannot "
        "invoke doctor scheduling endpoints and doctors cannot manipulate unassigned patient records.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec5_4", tracker))
    story.append(Paragraph("<b>5.4 Email & Notifications: Spring Boot Starter Mail & SHA-256 Tokens</b>", styles['h1']))
    story.append(Paragraph(
        "Account authenticity is guaranteed via <b>Spring Boot Starter Mail</b> (<code>JavaMailSender</code>) over configurable SMTP. "
        "Rather than persisting raw verification tokens in the database, MediGuide Care generates a cryptographically random 32-byte Base64URL string, "
        "computes its 64-character SHA-256 hash, and stores only the hash in <code>email_verification_tokens</code>. "
        "Tokens expire automatically after 24 hours and are deleted immediately upon single-use verification. "
        "A 60-second rate-limiting cooldown prevents email flooding, and uniform responses safeguard against user enumeration.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec5_5", tracker))
    story.append(Paragraph("<b>5.5 Frontend: React 19, Vite 8, and Modern Vanilla CSS</b>", styles['h1']))
    story.append(Paragraph(
        "The client layer is implemented as a Single-Page Application (SPA) using <b>React 19</b> and <b>Vite 8</b>. "
        "React's component model coordinates dynamic portal views (<code>DoctorPortal</code>, <code>PatientPortal</code>, <code>DoctorAvailabilityCalendar</code>, <code>DoctorClinicalDrawer</code>). "
        "Vite provides sub-second Hot Module Replacement (HMR) and optimized rollup production bundles (341 kB minified JS). "
        "The interface avoids bulky CSS utility frameworks, instead employing a custom <b>Vanilla CSS design system</b> "
        "defined in <code>index.css</code> and <code>RolePortal.css</code>. It utilizes semantic design tokens, clean glassmorphism accents, "
        "and accessible typography (Google Fonts Outfit and Inter) tailored for medical clinic environments.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec5_6", tracker))
    story.append(Paragraph("<b>5.6 External AI Integration: Spring WebClient</b>", styles['h1']))
    story.append(Paragraph(
        "The clinical history summarizer integrates with OpenAI-compatible large language model APIs via a non-blocking <code>WebClient</code> wrapper "
        "(<code>OpenAiCompatibleClinicalSummaryClient</code>). The integration is strictly isolated: prompts contain only chronological consultation data "
        "with internal keys stripped, subject to strict medical safety system instructions and bounded token budgets. "
        "In test environments, deterministic mocks prevent outbound HTTP network calls, guaranteeing reliable builds.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec5_7", tracker))
    story.append(Paragraph("<b>5.7 API Tooling: SpringDoc OpenAPI 3 and Swagger UI</b>", styles['h1']))
    story.append(Paragraph(
        "Comprehensive API documentation is generated dynamically at runtime using <b>springdoc-openapi 2.5.0</b>. "
        "The interactive Swagger UI (accessible at <code>/swagger-ui.html</code>) and OpenAPI JSON schema (<code>/api-docs</code>) "
        "allow developers and evaluators to inspect schema definitions, review request parameters, and execute authenticated API calls "
        "by supplying the JWT Bearer token directly through the UI authorize modal.",
        styles['body']
    ))
    story.append(PageBreak())

    # ==========================================================================
    # CHAPTER 6: SYSTEM ANALYSIS AND DESIGN
    # ==========================================================================
    story.append(TrackedBookmark("ch6", tracker))
    story.append(Paragraph("<b>CHAPTER 6: SYSTEM ANALYSIS AND DESIGN</b>", styles['chapter']))
    story.append(Spacer(1, 10))

    story.append(TrackedBookmark("sec6_1", tracker))
    story.append(Paragraph("<b>6.1 Architectural Principles and Separation of Concerns</b>", styles['h1']))
    story.append(Paragraph(
        "MediGuide Care strictly adheres to core software engineering principles: Separation of Concerns (SoC), Single Responsibility Principle (SRP), "
        "and the Principle of Least Privilege. The codebase is decoupled into distinct vertical tiers: the client layer manages user interaction and token storage; "
        "the security gateway authenticates incoming HTTP requests; controllers validate DTO syntax; services execute business logic and concurrency locks; "
        "and repositories manage database transactions. No database queries exist in controllers, and no presentation markup exists in backend services.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec6_2", tracker))
    story.append(Paragraph("<b>6.2 Multi-Tier Application Design Pattern</b>", styles['h1']))
    story.append(Paragraph(
        "The backend follows the established enterprise <b>Controller → Service → Repository</b> architectural pattern:",
        styles['body']
    ))
    story.append(Paragraph("1. <b>REST Controllers:</b> Accept incoming HTTP requests, unpack JSON bodies into typed DTOs, invoke Jakarta Bean Validation (<code>@Valid</code>), and return standard HTTP status codes (200 OK, 201 Created, 400 Bad Request, 409 Conflict).", styles['bullet']))
    story.append(Paragraph("2. <b>Service Layer:</b> Contains all business rules, role authorization checks, pessimistic concurrency locks, cryptographic token generation, and transactional boundaries (<code>@Transactional</code>).", styles['bullet']))
    story.append(Paragraph("3. <b>Repository Layer:</b> Extends <code>JpaRepository</code> to execute parameterized SQL and JPQL queries against PostgreSQL, managing entity lifecycles and relationship fetching.", styles['bullet']))

    story.append(TrackedBookmark("sec6_3", tracker))
    story.append(Paragraph("<b>6.3 Data Transfer Object (DTO) Boundary Pattern</b>", styles['h1']))
    story.append(Paragraph(
        "Domain entities are never exposed directly to external HTTP clients. MediGuide Care enforces a strict DTO boundary pattern "
        "to prevent accidental data leakage, cyclic JSON serialization, and mass-assignment vulnerabilities. "
        "Separate request DTOs (e.g., <code>RegisterRequest</code>, <code>BookAppointmentRequest</code>, <code>CreateConsultationRequest</code>) "
        "and response DTOs (e.g., <code>AuthResponse</code>, <code>AppointmentResponse</code>, <code>PublicDoctorProfileResponse</code>) "
        "ensure that passwords, internal database auto-increment keys, and confidential phone numbers are filtered out before transmission.",
        styles['body']
    ))

    story.append(TrackedBookmark("sec6_4", tracker))
    story.append(Paragraph("<b>6.4 Concurrency Control & Pessimistic Row Locking Strategy</b>", styles['h1']))
    story.append(Paragraph(
        "Preventing appointment double-booking under concurrent client requests is a critical engineering requirement. "
        "In a naive implementation, checking slot availability and subsequently inserting an appointment introduces a race condition: "
        "two concurrent threads can both observe the slot as free, and both will proceed to insert conflicting records.",
        styles['body']
    ))
    story.append(Paragraph(
        "MediGuide Care solves this deterministically through a multi-stage concurrency coordination algorithm in <code>AppointmentService</code>:",
        styles['body']
    ))
    story.append(Paragraph("1. <b>Deterministic Lock Acquisition Order:</b> The service identifies the IDs of both the patient and the doctor. To prevent distributed deadlocks where Thread A locks Patient then Doctor while Thread B locks Doctor then Patient, locks are <b>always acquired in ascending User ID order</b> (<code>min(patientId, doctorId)</code> followed by <code>max(patientId, doctorId)</code>).", styles['bullet']))
    story.append(Paragraph("2. <b>Pessimistic Row-Level Write Locks:</b> The service executes <code>userRepository.findByIdForUpdate(id)</code>, issuing SQL <code>SELECT ... FOR UPDATE</code>. This serializes concurrent booking requests for either the doctor or the patient at the database level.", styles['bullet']))
    story.append(Paragraph("3. <b>Transactional Entity Refresh:</b> Under the active lock, the doctor availability entity is refreshed from the database (<code>entityManager.refresh(availability)</code>) to observe any concurrent schedule deactivations.", styles['bullet']))
    story.append(Paragraph("4. <b>SQL Overlap Range Query:</b> The engine queries for existing appointments in <code>BOOKED</code> status overlapping the requested interval using half-open time interval logic: "
        "<code>WHERE doctor_id = :doctorId AND status = 'BOOKED' AND start_time &lt; :newEndTime AND end_time &gt; :newStartTime</code>.", styles['bullet']))
    story.append(Paragraph("5. <b>Conflict Resolution:</b> If any conflict is detected, the transaction aborts and returns an HTTP <code>409 Conflict</code> with an explanatory message; otherwise, the appointment is created atomically.", styles['bullet']))

    story.append(TrackedBookmark("sec6_5", tracker))
    story.append(Paragraph("<b>6.5 Data Privacy Model: Segregating Public Clinic vs. Private Personal Information</b>", styles['h1']))
    story.append(Paragraph(
        "MediGuide Care implements a robust three-tier data privacy model that eliminates unauthorized disclosure of personal phone numbers:",
        styles['body']
    ))
    story.append(Paragraph("• <b>Patient Contact Number (<code>users.phone_number</code>):</b> Strictly private. Accessible and editable exclusively by the authenticated patient via <code>GET/PUT /api/v1/patient/profile</code>. It is completely excluded from doctor search results, public lookup endpoints, and unauthenticated views.", styles['bullet']))
    story.append(Paragraph("• <b>Doctor Clinic Phone (<code>doctor_profiles.clinic_phone</code>):</b> Public operational contact. Displayed on public doctor profile cards and search results so prospective patients can make clinic inquiries.", styles['bullet']))
    story.append(Paragraph("• <b>Doctor Personal Phone (<code>doctor_profiles.personal_phone</code>):</b> Strictly confidential. Returned only to the authenticated doctor in <code>DoctorProfileResponse</code> when managing their own profile. It is completely stripped from <code>PublicDoctorProfileResponse</code> and public search results.", styles['bullet']))
    story.append(Paragraph("• <b>Public MediGuide IDs (<code>DOC-XXXXXX</code> / <code>PAT-XXXXXX</code>):</b> Replaces internal database integer IDs on all client responses, preventing attackers from probing or enumerating total clinic registration counts.", styles['bullet']))
    story.append(PageBreak())

    return story

print("Chapters 1 to 6 builder ready.")
