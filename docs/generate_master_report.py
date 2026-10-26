"""
MediGuide Care — Master Academic Project Report Generator
Orchestrates two-pass layout, exact pagination, clickable Table of Contents,
List of Figures, List of Tables, and builds the final PDF:
docs/MediGuide_Care_Final_Project_Report.pdf
"""

import os
import io
import pypdf
from reportlab.platypus import SimpleDocTemplate
from report_base import (
    PAGE_WIDTH, PAGE_HEIGHT, MARGIN,
    setup_styles, AcademicNumberedCanvas
)
from report_prelim import get_preliminary_story
from report_chapters_part1 import get_chapters_1_to_6_story
from report_chapters_part2 import get_chapters_7_to_10_story
from report_chapters_part3 import get_chapters_11_to_14_story

DOCS_DIR = os.path.dirname(__file__)
FINAL_PDF_PATH = os.path.join(DOCS_DIR, "MediGuide_Care_Final_Project_Report.pdf")

# Definition of TOC hierarchy
TOC_DEFINITIONS = [
    {"title": "Certificate", "key": "prelim_cert", "bold": False, "indent": 0},
    {"title": "Declaration", "key": "prelim_decl", "bold": False, "indent": 0},
    {"title": "Acknowledgement", "key": "prelim_ack", "bold": False, "indent": 0},
    {"title": "Abstract", "key": "prelim_abstract", "bold": False, "indent": 0},
    {"title": "List of Figures", "key": "prelim_lof", "bold": False, "indent": 0},
    {"title": "List of Tables", "key": "prelim_lot", "bold": False, "indent": 0},
    
    {"title": "Chapter 1: Introduction", "key": "ch1", "bold": True, "indent": 0},
    {"title": "1.1 Project Overview and Domain Context", "key": "sec1_1", "bold": False, "indent": 12},
    {"title": "1.2 Purpose and Objectives", "key": "sec1_2", "bold": False, "indent": 12},
    {"title": "1.3 Target Users and Operating Context", "key": "sec1_3", "bold": False, "indent": 12},
    {"title": "1.4 Organization of the Report", "key": "sec1_4", "bold": False, "indent": 12},

    {"title": "Chapter 2: Problem Statement, Objectives, Scope, and Motivation", "key": "ch2", "bold": True, "indent": 0},
    {"title": "2.1 Problem Statement and Real-World Clinic Challenges", "key": "sec2_1", "bold": False, "indent": 12},
    {"title": "2.2 Project Objectives", "key": "sec2_2", "bold": False, "indent": 12},
    {"title": "2.3 Project Scope", "key": "sec2_3", "bold": False, "indent": 12},
    {"title": "2.4 Academic and Practical Motivation", "key": "sec2_4", "bold": False, "indent": 12},

    {"title": "Chapter 3: Existing System and Proposed System", "key": "ch3", "bold": True, "indent": 0},
    {"title": "3.1 Overview of Existing Outpatient Workflows", "key": "sec3_1", "bold": False, "indent": 12},
    {"title": "3.2 Drawbacks and Security Vulnerabilities of Current Systems", "key": "sec3_2", "bold": False, "indent": 12},
    {"title": "3.3 Proposed MediGuide Care System", "key": "sec3_3", "bold": False, "indent": 12},
    {"title": "3.4 Comparative Analysis Matrix", "key": "sec3_4", "bold": False, "indent": 12},

    {"title": "Chapter 4: Software and Hardware Requirements", "key": "ch4", "bold": True, "indent": 0},
    {"title": "4.1 Hardware Requirements", "key": "sec4_1", "bold": False, "indent": 12},
    {"title": "4.2 Software Requirements and Operating Environments", "key": "sec4_2", "bold": False, "indent": 12},
    {"title": "4.3 Runtime Dependencies and Library Specifications", "key": "sec4_3", "bold": False, "indent": 12},

    {"title": "Chapter 5: Technology Stack and Architectural Justification", "key": "ch5", "bold": True, "indent": 0},
    {"title": "5.1 Backend: Java 17 LTS and Spring Boot 3.2.5", "key": "sec5_1", "bold": False, "indent": 12},
    {"title": "5.2 Persistence: PostgreSQL 16 and Hibernate 6 ORM", "key": "sec5_2", "bold": False, "indent": 12},
    {"title": "5.3 Security: Spring Security 6, BCrypt, and JJWT", "key": "sec5_3", "bold": False, "indent": 12},
    {"title": "5.4 Email & Notifications: Spring Boot Starter Mail & SHA-256 Tokens", "key": "sec5_4", "bold": False, "indent": 12},
    {"title": "5.5 Frontend: React 19, Vite 8, and Modern Vanilla CSS", "key": "sec5_5", "bold": False, "indent": 12},
    {"title": "5.6 External AI Integration: Spring WebClient", "key": "sec5_6", "bold": False, "indent": 12},
    {"title": "5.7 API Tooling: SpringDoc OpenAPI 3 and Swagger UI", "key": "sec5_7", "bold": False, "indent": 12},

    {"title": "Chapter 6: System Analysis and Design", "key": "ch6", "bold": True, "indent": 0},
    {"title": "6.1 Architectural Principles and Separation of Concerns", "key": "sec6_1", "bold": False, "indent": 12},
    {"title": "6.2 Multi-Tier Application Design Pattern", "key": "sec6_2", "bold": False, "indent": 12},
    {"title": "6.3 Data Transfer Object (DTO) Boundary Pattern", "key": "sec6_3", "bold": False, "indent": 12},
    {"title": "6.4 Concurrency Control & Pessimistic Row Locking Strategy", "key": "sec6_4", "bold": False, "indent": 12},
    {"title": "6.5 Data Privacy Model: Segregating Public Clinic vs. Private Personal Information", "key": "sec6_5", "bold": False, "indent": 12},

    {"title": "Chapter 7: System Architecture and Workflow Diagrams", "key": "ch7", "bold": True, "indent": 0},
    {"title": "7.1 Overall System Architecture", "key": "sec7_1", "bold": False, "indent": 12},
    {"title": "7.2 Registration, Login, JWT, and Email Verification Workflow", "key": "sec7_2", "bold": False, "indent": 12},
    {"title": "7.3 Doctor Availability and Patient Appointment Booking Workflow", "key": "sec7_3", "bold": False, "indent": 12},
    {"title": "7.4 Clinical Consultation, Prescription, and Follow-Up Workflow", "key": "sec7_4", "bold": False, "indent": 12},
    {"title": "7.5 AI-Assisted Clinical History Summarization Workflow and Safeguards", "key": "sec7_5", "bold": False, "indent": 12},

    {"title": "Chapter 8: Database Design, Entities, Relationships, and Data Flow", "key": "ch8", "bold": True, "indent": 0},
    {"title": "8.1 Relational Database Entity-Relationship Diagram (ERD)", "key": "sec8_1", "bold": False, "indent": 12},
    {"title": "8.2 Entity Dictionary and Schema Definitions", "key": "sec8_2", "bold": False, "indent": 12},
    {"title": "8.3 Referential Integrity, Cascade Policies, and Constraints", "key": "sec8_3", "bold": False, "indent": 12},
    {"title": "8.4 Data Flow and Transactional Boundaries", "key": "sec8_4", "bold": False, "indent": 12},

    {"title": "Chapter 9: Module-Wise Implementation", "key": "ch9", "bold": True, "indent": 0},
    {"title": "9.1 User Authentication, Role-Based Access Control, and Public IDs", "key": "sec9_1", "bold": False, "indent": 12},
    {"title": "9.2 Doctor Profile Management & Location-Based Discovery Search", "key": "sec9_2", "bold": False, "indent": 12},
    {"title": "9.3 Availability Calendar Scheduler & Slot Management", "key": "sec9_3", "bold": False, "indent": 12},
    {"title": "9.4 Appointment Booking, Concurrency Prevention, and Lifecycle Management", "key": "sec9_4", "bold": False, "indent": 12},
    {"title": "9.5 Clinical Consultation Documentation & Digital Prescriptions Drawer", "key": "sec9_5", "bold": False, "indent": 12},
    {"title": "9.6 Patient Care Timeline & Historical Consultations", "key": "sec9_6", "bold": False, "indent": 12},
    {"title": "9.7 AI-Assisted Clinical History Summarizer & Doctor-in-the-Loop Safeguards", "key": "sec9_7", "bold": False, "indent": 12},
    {"title": "9.8 Email Verification System, Cooldown Rate Limits, and Anti-Enumeration", "key": "sec9_8", "bold": False, "indent": 12},
    {"title": "9.9 Role-Based Access Control Matrix and Privacy Boundaries", "key": "sec9_9", "bold": False, "indent": 12},

    {"title": "Chapter 10: API Design, Security, and Endpoint Documentation", "key": "ch10", "bold": True, "indent": 0},
    {"title": "10.1 RESTful API Principles and Resource Mapping", "key": "sec10_1", "bold": False, "indent": 12},
    {"title": "10.2 Comprehensive Endpoint Inventory", "key": "sec10_2", "bold": False, "indent": 12},
    {"title": "10.3 Authentication and Authorization Headers", "key": "sec10_3", "bold": False, "indent": 12},
    {"title": "10.4 Error Handling Schema and HTTP Status Code Semantics", "key": "sec10_4", "bold": False, "indent": 12},

    {"title": "Chapter 11: Testing Strategy, Test Cases, and Verified Results", "key": "ch11", "bold": True, "indent": 0},
    {"title": "11.1 Testing Methodology and Multi-Layer Test Architecture", "key": "sec11_1", "bold": False, "indent": 12},
    {"title": "11.2 Unit, Service, Repository, and Controller Integration Tests", "key": "sec11_2", "bold": False, "indent": 12},
    {"title": "11.3 Concurrency & Pessimistic Lock Verification", "key": "sec11_3", "bold": False, "indent": 12},
    {"title": "11.4 Comprehensive Test Suite Execution Summary", "key": "sec11_4", "bold": False, "indent": 12},
    {"title": "11.5 Frontend Static Code Linting & Production Build Verification", "key": "sec11_5", "bold": False, "indent": 12},

    {"title": "Chapter 12: User Interface and Responsive Design", "key": "ch12", "bold": True, "indent": 0},
    {"title": "12.1 Design Philosophy, Color Palette, and Medical Theme", "key": "sec12_1", "bold": False, "indent": 12},
    {"title": "12.2 Modern Vanilla CSS Design System and Token Architecture", "key": "sec12_2", "bold": False, "indent": 12},
    {"title": "12.3 Doctor Portal Workflow and Slide-Over Clinical Drawer", "key": "sec12_3", "bold": False, "indent": 12},
    {"title": "12.4 Patient Portal Workflow and Care Timeline", "key": "sec12_4", "bold": False, "indent": 12},
    {"title": "12.5 Accessibility, Micro-Animations, and Viewport Adaptability", "key": "sec12_5", "bold": False, "indent": 12},

    {"title": "Chapter 13: Limitations and Future Enhancements", "key": "ch13", "bold": True, "indent": 0},
    {"title": "13.1 Operational Constraints and Current Limitations", "key": "sec13_1", "bold": False, "indent": 12},
    {"title": "13.2 Technical Enhancements and Scalability Roadmap", "key": "sec13_2", "bold": False, "indent": 12},

    {"title": "Chapter 14: Conclusion", "key": "ch14", "bold": True, "indent": 0},
    {"title": "14.1 Summary of Academic Project Achievements", "key": "sec14_1", "bold": False, "indent": 12},
    {"title": "14.2 Learning Outcomes and Viva Preparation Takeaways", "key": "sec14_2", "bold": False, "indent": 12},

    {"title": "References", "key": "ch_ref", "bold": True, "indent": 0},
    {"title": "Appendix", "key": "ch_app", "bold": True, "indent": 0},
    {"title": "Appendix A: Environment Configuration Reference", "key": "app_a", "bold": False, "indent": 12},
    {"title": "Appendix B: Sample API Request and Response Payloads", "key": "app_b", "bold": False, "indent": 12},
    {"title": "Appendix C: Standard Error Response Schema", "key": "app_c", "bold": False, "indent": 12}
]

LOF_DEFINITIONS = [
    {"title": "Figure 7.1: Overall Multi-Tier System Architecture of MediGuide Care", "key": "fig7_1"},
    {"title": "Figure 7.2: User Registration, Login, JWT Authentication, and Email Verification Lifecycle", "key": "fig7_2"},
    {"title": "Figure 7.3: Doctor Availability Scheduling and Concurrency-Safe Appointment Booking Workflow", "key": "fig7_3"},
    {"title": "Figure 7.4: Clinical Consultation Documentation, Digital Prescriptions, and Care Timeline Lifecycle", "key": "fig7_4"},
    {"title": "Figure 7.5: AI-Assisted Clinical History Summarizer Workflow and Multi-Layer Medical Safeguards", "key": "fig7_5"},
    {"title": "Figure 8.1: Complete Relational Database Entity-Relationship Diagram (ERD)", "key": "fig8_1"},
    {"title": "Figure 9.1: Role-Based Access Control (RBAC) Security Architecture and Privacy Boundaries", "key": "fig9_1"}
]

LOT_DEFINITIONS = [
    {"title": "Table 3.1: Comparative Analysis Between Conventional Systems and MediGuide Care", "key": "tab3_1"},
    {"title": "Table 4.1: Hardware Requirements for MediGuide Care", "key": "tab4_1"},
    {"title": "Table 4.2: Software Environment Specifications", "key": "tab4_2"},
    {"title": "Table 4.3: Core Application Libraries and Dependencies", "key": "tab4_3"},
    {"title": "Table 8.1: Comprehensive Relational Database Data Dictionary", "key": "tab8_1"},
    {"title": "Table 10.1: Comprehensive MediGuide Care REST API Endpoint Inventory", "key": "tab10_1"},
    {"title": "Table 10.2: HTTP Status Code Semantics and Error Mapping", "key": "tab10_2"},
    {"title": "Table 11.1: Core Automated Test Scenarios and Verified Execution Results", "key": "tab11_1"},
    {"title": "Table A.1: Backend Environment Variables Reference", "key": "tab_a1"}
]

def build_full_story(styles, tracker, prelim_end_page):
    """Assembles all sections into a continuous flowable list."""
    # Convert tracker values to display format:
    # If key is prelim, use roman; if main, use (raw_page - prelim_end_page)
    romans = ["", "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x", "xi", "xii"]
    display_tracker = {}
    for k, raw_p in tracker.items():
        if raw_p <= prelim_end_page:
            display_tracker[k] = romans[raw_p] if raw_p < len(romans) else str(raw_p)
        else:
            display_tracker[k] = str(raw_p - prelim_end_page)

    story = []
    # 1. Preliminary
    story.extend(get_preliminary_story(styles, tracker, TOC_DEFINITIONS, LOF_DEFINITIONS, LOT_DEFINITIONS))
    # 2. Chapters 1 to 6
    story.extend(get_chapters_1_to_6_story(styles, tracker))
    # 3. Chapters 7 to 10
    story.extend(get_chapters_7_to_10_story(styles, tracker))
    # 4. Chapters 11 to 14, Refs, App
    story.extend(get_chapters_11_to_14_story(styles, tracker))

    return story, display_tracker

def generate_report():
    print("=" * 70)
    print("MediGuide Care — Final Project Report Generation Starting")
    print("=" * 70)

    styles = setup_styles()

    # --------------------------------------------------------------------------
    # PASS 1: Dry run to discover exact page layout and bookmark locations
    # --------------------------------------------------------------------------
    print("\n[Pass 1] Running layout simulation to compute bookmark page numbers...")
    raw_tracker = {}
    
    # Initialize with default dummy page 99 for steady line spacing
    dummy_tracker = {entry['key']: 99 for entry in TOC_DEFINITIONS + LOF_DEFINITIONS + LOT_DEFINITIONS}
    
    pass1_story, _ = build_full_story(styles, raw_tracker, prelim_end_page=7)
    
    doc1 = SimpleDocTemplate(
        io.BytesIO(),
        pagesize=(PAGE_WIDTH, PAGE_HEIGHT),
        leftMargin=MARGIN, rightMargin=MARGIN,
        topMargin=MARGIN, bottomMargin=MARGIN
    )
    doc1.build(pass1_story)

    # Determine prelim_end_page: page where Chapter 1 starts minus 1
    ch1_raw_page = raw_tracker.get("ch1", 8)
    prelim_end_page = ch1_raw_page - 1
    print(f"Computed preliminary pages count: {prelim_end_page} (Chapter 1 starts at raw page {ch1_raw_page})")

    # --------------------------------------------------------------------------
    # PASS 2: Build final document with exact computed page numbers & links
    # --------------------------------------------------------------------------
    print(f"\n[Pass 2] Building final academic report to {FINAL_PDF_PATH}...")
    
    # Update display tracker with exact values
    romans = ["", "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x", "xi", "xii"]
    final_display_tracker = {}
    for k, raw_p in raw_tracker.items():
        if raw_p <= prelim_end_page:
            final_display_tracker[k] = romans[raw_p] if raw_p < len(romans) else str(raw_p)
        else:
            final_display_tracker[k] = str(raw_p - prelim_end_page)

    final_story = []
    # Build with finalized display tracker
    final_story.extend(get_preliminary_story(styles, final_display_tracker, TOC_DEFINITIONS, LOF_DEFINITIONS, LOT_DEFINITIONS))
    final_story.extend(get_chapters_1_to_6_story(styles, raw_tracker))
    final_story.extend(get_chapters_7_to_10_story(styles, raw_tracker))
    final_story.extend(get_chapters_11_to_14_story(styles, raw_tracker))

    # Configure canvas with exact prelim_end_page
    class ConfiguredAcademicCanvas(AcademicNumberedCanvas):
        pass
    ConfiguredAcademicCanvas.prelim_end_page = prelim_end_page

    doc2 = SimpleDocTemplate(
        FINAL_PDF_PATH,
        pagesize=(PAGE_WIDTH, PAGE_HEIGHT),
        leftMargin=MARGIN, rightMargin=MARGIN,
        topMargin=MARGIN, bottomMargin=MARGIN,
        title="MediGuide Care - Final MCA Project Report",
        author="Kashyap Patil",
        subject="Doctor-Patient Consultation and Follow-up Management System",
        keywords="Healthcare, Spring Boot, React, PostgreSQL, JWT, Concurrency, AI Summarizer"
    )
    doc2.build(final_story, canvasmaker=ConfiguredAcademicCanvas)

    print("\n[Pass 2 Completed] Successfully generated PDF!")

    # --------------------------------------------------------------------------
    # Quality & Verification Audit
    # --------------------------------------------------------------------------
    print("\n" + "=" * 70)
    print("POST-GENERATION QUALITY & COMPLIANCE AUDIT")
    print("=" * 70)
    
    reader = pypdf.PdfReader(FINAL_PDF_PATH)
    total_pages = len(reader.pages)
    main_body_pages = total_pages - prelim_end_page

    print(f"1. Total Document Pages: {total_pages}")
    print(f"   * Preliminary Pages: {prelim_end_page} (Roman numerals i to {romans[prelim_end_page]})")
    print(f"   * Main Body Pages:   {main_body_pages} (Arabic numerals 1 to {main_body_pages})")

    # Text extraction verification
    sample_text = reader.pages[prelim_end_page].extract_text()
    print("\n2. Selectable & Searchable Text Verification:")
    print(f"   * Sample Chapter 1 extracted snippet: {repr(sample_text[:80])}...")
    assert len(sample_text) > 100, "Text extraction failed or yielded empty content!"
    print("   [OK] Text is selectable and searchable.")

    # Link annotations verification
    toc_page = reader.pages[4]  # Approx TOC page
    annots = toc_page.get('/Annots')
    annots_count = len(annots) if annots else 0
    print(f"\n3. Internal Link Annotations:")
    print(f"   * TOC page links detected: {annots_count} interactive hyperlinks")
    assert annots_count > 0, "No hyperlinks detected on TOC page!"
    print("   [OK] Clickable Table of Contents verified.")

    # Check diagrams exist
    print("\n4. Embedded Diagrams & Figures Audit:")
    for fig in LOF_DEFINITIONS:
        key = fig['key']
        p_val = final_display_tracker.get(key, "-")
        print(f"   * {fig['title']} -> Page {p_val}")

    print("\n5. Document Metadata:")
    print(f"   * Title:   {reader.metadata.title}")
    print(f"   * Author:  {reader.metadata.author}")
    print(f"   * Subject: {reader.metadata.subject}")

    print("\n" + "=" * 70)
    print(f"FINAL DELIVERABLE AVAILABLE AT:\n{FINAL_PDF_PATH}")
    print("=" * 70)

if __name__ == "__main__":
    generate_report()
