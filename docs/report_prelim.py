"""
MediGuide Care — Final Project Report Content Builder
Constructs all flowables for preliminary pages, chapters 1 to 14, references, and appendices.
"""

import os
from reportlab.platypus import (
    Paragraph, PageBreak, Spacer, Table, TableStyle, Image, KeepTogether, HRFlowable
)
from reportlab.lib import colors
from reportlab.lib.units import inch
from reportlab.lib.styles import ParagraphStyle
from report_base import (
    PAGE_WIDTH, PAGE_HEIGHT, MARGIN, USABLE_WIDTH,
    COLOR_PRIMARY, COLOR_SECONDARY, COLOR_TEXT, COLOR_MUTED, COLOR_BORDER, COLOR_LIGHT_BG,
    TrackedBookmark, format_toc_line, format_academic_table, create_code_box
)

DIAGRAMS_DIR = os.path.join(os.path.dirname(__file__), "diagrams")

def get_preliminary_story(styles, tracker, toc_entries, lof_entries, lot_entries):
    """Generates Preliminary Pages: Title, Certificate, Declaration, Acknowledgement, Abstract, TOC, LOF, LOT."""
    story = []

    # ==========================================================================
    # 1. TITLE PAGE (Page 1)
    # ==========================================================================
    story.append(TrackedBookmark("prelim_title", tracker))
    story.append(Spacer(1, 20))
    
    # Institution & Department Header
    inst_header = ParagraphStyle('InstHeader', parent=styles['chapter'], alignment=1, fontSize=13, leading=18, textColor=COLOR_PRIMARY)
    story.append(Paragraph("<b>DEPARTMENT OF COMPUTER APPLICATIONS</b>", inst_header))
    story.append(Paragraph("<b>[INSTITUTION / COLLEGE NAME PLACEHOLDER]</b>", ParagraphStyle('InstSub', parent=inst_header, fontSize=11, leading=16, textColor=COLOR_MUTED)))
    story.append(Spacer(1, 25))

    # Project Title
    title_style = ParagraphStyle('RepTitle', parent=styles['chapter'], alignment=1, fontSize=18, leading=24, textColor=COLOR_PRIMARY)
    story.append(Paragraph("<b>MEDIGUIDE CARE</b>", title_style))
    sub_title_style = ParagraphStyle('RepSubTitle', parent=styles['chapter'], alignment=1, fontSize=13, leading=19, textColor=COLOR_SECONDARY)
    story.append(Paragraph("<b>A Doctor–Patient Consultation and Follow-up Management System</b>", sub_title_style))
    story.append(Spacer(1, 15))

    p_desc = ParagraphStyle('RepDesc', parent=styles['body'], alignment=1, fontSize=11, leading=16, fontName='TimesNewRoman-Italic')
    story.append(Paragraph("A Mini-Project Report submitted in partial fulfillment of the requirements<br/>for the award of the degree of", p_desc))
    story.append(Spacer(1, 10))

    deg_style = ParagraphStyle('RepDeg', parent=styles['chapter'], alignment=1, fontSize=14, leading=20, textColor=COLOR_PRIMARY)
    story.append(Paragraph("<b>MASTER OF COMPUTER APPLICATIONS (MCA)</b>", deg_style))
    story.append(Spacer(1, 35))

    # Student and Guide details in a 2-column table
    left_meta = Paragraph(
        "<b>Submitted by:</b><br/>"
        "<b>Kashyap Patil</b><br/>"
        "Roll No / USN: [Roll Number Placeholder]<br/>"
        "MCA Semester IV<br/>"
        "Academic Year: 2025 – 2026",
        styles['table_cell']
    )
    right_meta = Paragraph(
        "<b>Under the Guidance of:</b><br/>"
        "<b>[Project Guide Name Placeholder]</b><br/>"
        "Designation: Assistant Professor<br/>"
        "Department of Computer Applications<br/>"
        "[Institution Name Placeholder]",
        styles['table_cell']
    )
    meta_table = Table([[left_meta, right_meta]], colWidths=[USABLE_WIDTH * 0.5, USABLE_WIDTH * 0.5])
    meta_table.setStyle(TableStyle([
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('LEFTPADDING', (0, 0), (-1, -1), 10),
        ('RIGHTPADDING', (0, 0), (-1, -1), 10),
    ]))
    story.append(meta_table)
    story.append(Spacer(1, 45))

    # Date / Month Footer
    date_style = ParagraphStyle('RepDate', parent=styles['body'], alignment=1, fontSize=11, leading=15, textColor=COLOR_MUTED)
    story.append(Paragraph("October 2026", date_style))
    story.append(PageBreak())

    # ==========================================================================
    # 2. CERTIFICATE (Page ii)
    # ==========================================================================
    story.append(TrackedBookmark("prelim_cert", tracker))
    story.append(Paragraph("<b>CERTIFICATE</b>", ParagraphStyle('CertHead', parent=styles['chapter'], alignment=1)))
    story.append(Spacer(1, 15))
    story.append(Paragraph(
        "This is to certify that the mini-project report entitled <b>“MediGuide Care: Doctor–Patient Consultation and Follow-up Management System”</b> "
        "is a bonafide record of work carried out by <b>Kashyap Patil</b> (Roll No: [Roll Number Placeholder]) in partial fulfillment of the requirements "
        "for the award of the degree of <b>Master of Computer Applications (MCA)</b> by [University / Institution Name Placeholder] during the academic year 2025 – 2026.",
        styles['body']
    ))
    story.append(Paragraph(
        "The project work described in this report has been carried out under our supervision and guidance, and has not formed the basis for the award of any other degree, diploma, associate-ship, or similar title previously.",
        styles['body']
    ))
    story.append(Spacer(1, 60))

    # Signature Blocks
    sig_cells = [
        [
            Paragraph("<b>________________________</b><br/><b>[Project Guide Name]</b><br/>Internal Project Guide<br/>Dept. of Computer Applications", styles['table_cell']),
            Paragraph("<b>________________________</b><br/><b>[Head of Department Name]</b><br/>Head of Department<br/>Dept. of Computer Applications", styles['table_cell'])
        ],
        [
            Paragraph("<br/><br/><b>________________________</b><br/><b>Internal Examiner</b><br/>Date: _________________", styles['table_cell']),
            Paragraph("<br/><br/><b>________________________</b><br/><b>External Examiner</b><br/>Date: _________________", styles['table_cell'])
        ]
    ]
    sig_table = Table(sig_cells, colWidths=[USABLE_WIDTH * 0.5, USABLE_WIDTH * 0.5])
    sig_table.setStyle(TableStyle([
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('LEFTPADDING', (0, 0), (-1, -1), 8),
        ('RIGHTPADDING', (0, 0), (-1, -1), 8),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 15),
    ]))
    story.append(sig_table)
    story.append(PageBreak())

    # ==========================================================================
    # 3. DECLARATION & ACKNOWLEDGEMENT (Page iii)
    # ==========================================================================
    story.append(TrackedBookmark("prelim_decl", tracker))
    story.append(Paragraph("<b>DECLARATION</b>", ParagraphStyle('DeclHead', parent=styles['chapter'], alignment=1)))
    story.append(Spacer(1, 10))
    story.append(Paragraph(
        "I hereby declare that the project work presented in this report entitled <b>“MediGuide Care: Doctor–Patient Consultation and Follow-up Management System”</b> "
        "is an authentic record of original work carried out by me under the guidance of <b>[Project Guide Name Placeholder]</b>, Department of Computer Applications, "
        "[Institution Name Placeholder]. I further declare that this report has not been submitted previously to any other university or institute for any degree or examination.",
        styles['body']
    ))
    story.append(Spacer(1, 20))
    story.append(Paragraph("Date: _________________<br/>Place: _________________", styles['body']))
    story.append(Paragraph("<b>Kashyap Patil</b><br/>MCA Final Year, Roll No: [Roll Number Placeholder]", ParagraphStyle('SignRight', parent=styles['body'], alignment=2)))
    story.append(Spacer(1, 25))

    story.append(TrackedBookmark("prelim_ack", tracker))
    story.append(Paragraph("<b>ACKNOWLEDGEMENT</b>", ParagraphStyle('AckHead', parent=styles['chapter'], alignment=1)))
    story.append(Spacer(1, 10))
    story.append(Paragraph(
        "I express my sincere gratitude to my project guide, <b>[Project Guide Name Placeholder]</b>, for continuous encouragement, valuable suggestions, "
        "and intellectual guidance throughout the design, implementation, and documentation of MediGuide Care.",
        styles['body']
    ))
    story.append(Paragraph(
        "I also express my heartfelt thanks to <b>[Head of Department Name Placeholder]</b>, Head of the Department of Computer Applications, "
        "and the faculty members of the institution for providing the required infrastructure, laboratory computing facilities, and constructive feedback.",
        styles['body']
    ))
    story.append(Paragraph(
        "Finally, I extend my appreciation to my family and fellow classmates for their moral support, collaboration, and insightful discussions during the development of this project.",
        styles['body']
    ))
    story.append(PageBreak())

    # ==========================================================================
    # 4. ABSTRACT (Page iv)
    # ==========================================================================
    story.append(TrackedBookmark("prelim_abstract", tracker))
    story.append(Paragraph("<b>ABSTRACT</b>", ParagraphStyle('AbsHead', parent=styles['chapter'], alignment=1)))
    story.append(Spacer(1, 12))
    story.append(Paragraph(
        "Outpatient medical clinics frequently grapple with fragmented scheduling, double-booked consultation intervals, unstructured paper-based prescriptions, "
        "and compromised patient privacy resulting from uncontrolled disclosure of personal phone numbers. <b>MediGuide Care</b> is a modern, full-stack, multi-tier web application "
        "engineered to resolve these operational deficiencies by unifying doctor discovery, availability planning, conflict-free appointment booking, digital consultation records, "
        "and AI-assisted clinical history summarization into a secure, cohesive platform.",
        styles['body']
    ))
    story.append(Paragraph(
        "The system is built upon a decoupled architecture comprising a single-page application client implemented with React 19 and Vite, a robust enterprise backend API "
        "powered by Java 17 and Spring Boot 3.2.5, and a transactional relational database persistence layer utilizing PostgreSQL 16 mapped via Spring Data JPA and Hibernate ORM. "
        "Security is enforced through stateless JSON Web Token (JWT) authentication signed with HMAC-SHA256, salted BCrypt password hashing, and fine-grained method-level Role-Based Access Control (RBAC). "
        "To safeguard user identity against sequential database enumeration, public-facing operations strictly utilize collision-resistant alphanumeric identifiers (<code>DOC-XXXXXX</code> and <code>PAT-XXXXXX</code>). "
        "A multi-stage email verification architecture—implemented with cryptographic 32-byte Base64URL tokens hashed with SHA-256 and subject to a 60-second rate-limiting cooldown—enforces progressive access control "
        "where high-trust operations such as appointment booking and schedule publication require confirmed accounts.",
        styles['body']
    ))
    story.append(Paragraph(
        "To guarantee zero appointment double-booking under concurrent traffic, the booking engine coordinates transactions via pessimistic row-level locking on ascending user identifiers and executes transactional range queries. "
        "Attending physicians document clinical findings, diagnoses, and structured digital prescriptions via a slide-over consultation drawer. An integrated OpenAI-compatible clinical summarizer extracts chronological patient records, "
        "strips sensitive identifiers, applies strict medical safety prompting rules, and yields non-binding draft summaries with doctor-in-the-loop verification disclaimers. "
        "The platform’s stability and functional correctness are substantiated by an automated backend test suite comprising <b>298 passing automated tests (100% pass rate)</b> and static linting with zero warnings.",
        styles['body']
    ))
    story.append(Paragraph(
        "<b>Keywords:</b> Healthcare Management, Spring Boot, React, PostgreSQL, JWT Authentication, Pessimistic Locking, Digital Prescriptions, AI Clinical Summarization, Email Verification.",
        ParagraphStyle('KwStyle', parent=styles['body'], fontName='TimesNewRoman-Italic')
    ))
    story.append(PageBreak())

    # ==========================================================================
    # 5. TABLE OF CONTENTS
    # ==========================================================================
    story.append(TrackedBookmark("prelim_toc", tracker))
    story.append(Paragraph("<b>TABLE OF CONTENTS</b>", ParagraphStyle('TocHead', parent=styles['chapter'], alignment=1)))
    story.append(Spacer(1, 10))

    # Column header row
    h_left = Paragraph("<b>Title</b>", styles['table_header'])
    h_right = Paragraph("<b>Page No.</b>", ParagraphStyle('R', parent=styles['table_header'], alignment=2))
    header_table = Table([[h_left, h_right]], colWidths=[USABLE_WIDTH - 55, 55])
    header_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), COLOR_PRIMARY),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
    ]))
    story.append(header_table)
    story.append(Spacer(1, 6))

    # Add TOC entries dynamically
    for entry in toc_entries:
        title = entry['title']
        key = entry['key']
        is_bold = entry.get('bold', False)
        indent = entry.get('indent', 0)
        page_val = tracker.get(key, entry.get('default_page', "—"))
        story.append(format_toc_line(title, str(page_val), key, styles, is_bold=is_bold, indent=indent))

    story.append(PageBreak())

    # ==========================================================================
    # 6. LIST OF FIGURES & LIST OF TABLES
    # ==========================================================================
    story.append(TrackedBookmark("prelim_lof", tracker))
    story.append(Paragraph("<b>LIST OF FIGURES</b>", ParagraphStyle('LofHead', parent=styles['chapter'], alignment=1)))
    story.append(Spacer(1, 10))
    story.append(header_table)
    story.append(Spacer(1, 6))

    for entry in lof_entries:
        title = entry['title']
        key = entry['key']
        page_val = tracker.get(key, entry.get('default_page', "—"))
        story.append(format_toc_line(title, str(page_val), key, styles, is_bold=False, indent=0))

    story.append(Spacer(1, 25))

    story.append(TrackedBookmark("prelim_lot", tracker))
    story.append(Paragraph("<b>LIST OF TABLES</b>", ParagraphStyle('LotHead', parent=styles['chapter'], alignment=1)))
    story.append(Spacer(1, 10))
    story.append(header_table)
    story.append(Spacer(1, 6))

    for entry in lot_entries:
        title = entry['title']
        key = entry['key']
        page_val = tracker.get(key, entry.get('default_page', "—"))
        story.append(format_toc_line(title, str(page_val), key, styles, is_bold=False, indent=0))

    story.append(PageBreak())

    return story

print("Preliminary story builder ready.")
