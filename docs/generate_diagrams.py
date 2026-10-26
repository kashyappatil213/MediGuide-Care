"""
Script to generate high-resolution, publication-quality academic architecture
and workflow diagrams for the MediGuide Care Final Project Report.
Uses Matplotlib to render clean vector-style diagrams at 300 DPI.
"""

import os
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.patches import FancyBboxPatch, ArrowStyle

OUT_DIR = os.path.join(os.path.dirname(__file__), "diagrams")
os.makedirs(OUT_DIR, exist_ok=True)

# Shared styling constants
FONT_FAMILY = "sans-serif"
COLOR_PRIMARY = "#1E3A8A"    # Deep Navy
COLOR_SECONDARY = "#0F766E"  # Deep Teal
COLOR_ACCENT = "#2563EB"     # Royal Blue
COLOR_BG_LIGHT = "#F8FAFC"   # Slate 50
COLOR_BORDER = "#475569"     # Slate 600
COLOR_TEXT = "#0F172A"       # Slate 900
COLOR_MUTED = "#64748B"      # Slate 500

def draw_box(ax, x, y, w, h, title, subtitle=None, bg="#FFFFFF", border="#334155", title_color="#0F172A", title_size=10, sub_size=8, corner=0.03):
    box = FancyBboxPatch(
        (x, y), w, h,
        boxstyle=f"round,pad=0.0,rounding_size={corner}",
        facecolor=bg, edgecolor=border, linewidth=1.2, zorder=2
    )
    ax.add_patch(box)
    
    if subtitle:
        ax.text(x + w/2, y + h*0.62, title, ha="center", va="center", fontsize=title_size, weight="bold", color=title_color, family=FONT_FAMILY, zorder=3)
        ax.text(x + w/2, y + h*0.28, subtitle, ha="center", va="center", fontsize=sub_size, color=COLOR_MUTED, family=FONT_FAMILY, zorder=3)
    else:
        ax.text(x + w/2, y + h/2, title, ha="center", va="center", fontsize=title_size, weight="bold", color=title_color, family=FONT_FAMILY, zorder=3)

def draw_arrow(ax, x1, y1, x2, y2, label=None, color="#334155", lw=1.3, label_pos=0.5, label_offset=(0, 0.02), label_size=8):
    ax.annotate(
        "", xy=(x2, y2), xytext=(x1, y1),
        arrowprops=dict(arrowstyle="-|>", color=color, lw=lw, mutation_scale=12, shrinkA=2, shrinkB=2),
        zorder=4
    )
    if label:
        mx = x1 + (x2 - x1) * label_pos + label_offset[0]
        my = y1 + (y2 - y1) * label_pos + label_offset[1]
        ax.text(mx, my, label, ha="center", va="bottom", fontsize=label_size, color=color, weight="bold", family=FONT_FAMILY, zorder=5,
                bbox=dict(boxstyle="square,pad=0.15", facecolor="#FFFFFF", edgecolor="none", alpha=0.85))

# ==============================================================================
# Diagram 1: Overall System Architecture
# ==============================================================================
def generate_diagram_1():
    fig, ax = plt.subplots(figsize=(10, 6.2), dpi=300)
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 7.5)
    ax.axis("off")

    # Title
    ax.text(5, 7.15, "Figure 7.1: Overall Multi-Tier System Architecture of MediGuide Care", ha="center", va="center", fontsize=11, weight="bold", color=COLOR_PRIMARY)

    # 1. Client Tier (Left Column)
    ax.add_patch(FancyBboxPatch((0.4, 0.6), 2.7, 6.2, boxstyle="round,pad=0.08,rounding_size=0.04", facecolor="#F0F9FF", edgecolor="#0284C7", lw=1.2, ls="--", zorder=1))
    ax.text(1.75, 6.55, "CLIENT TIER (React 19 + Vite)", ha="center", va="center", fontsize=9.5, weight="bold", color="#0369A1")
    
    draw_box(ax, 0.6, 5.5, 2.3, 0.75, "AuthCard Component", "Login & Registration UI", bg="#FFFFFF", border="#0284C7")
    draw_box(ax, 0.6, 4.4, 2.3, 0.85, "Doctor Portal", "Calendar, Queue, Drawer, Rx", bg="#FFFFFF", border="#0284C7")
    draw_box(ax, 0.6, 3.3, 2.3, 0.85, "Patient Portal", "Search, Booking, Timeline", bg="#FFFFFF", border="#0284C7")
    draw_box(ax, 0.6, 2.2, 2.3, 0.75, "RolePortal Router", "Client-Side RBAC Switch", bg="#FFFFFF", border="#0284C7")
    draw_box(ax, 0.6, 0.9, 2.3, 0.95, "apiClient (Axios/Fetch)", "Bearer JWT Injection & Interceptor", bg="#E0F2FE", border="#0284C7", title_size=9)

    # 2. Security Gateway & Controllers (Center-Left)
    ax.add_patch(FancyBboxPatch((3.5, 0.6), 2.9, 6.2, boxstyle="round,pad=0.08,rounding_size=0.04", facecolor="#F8FAFC", edgecolor="#475569", lw=1.2, ls="--", zorder=1))
    ax.text(4.95, 6.55, "API SECURITY & CONTROLLERS (Spring Boot)", ha="center", va="center", fontsize=9.5, weight="bold", color=COLOR_PRIMARY)

    draw_box(ax, 3.7, 5.5, 2.5, 0.75, "Spring Security 6 Gateway", "CORS Filter & Stateless Session", bg="#EEF2FF", border="#4F46E5")
    draw_box(ax, 3.7, 4.5, 2.5, 0.75, "JwtAuthenticationFilter", "HMAC-SHA256 Token Validation", bg="#EEF2FF", border="#4F46E5")
    
    draw_box(ax, 3.7, 3.5, 2.5, 0.75, "AuthController & ProfileCtrl", "Auth, Registration, Profiles", bg="#FFFFFF", border="#64748B")
    draw_box(ax, 3.7, 2.5, 2.5, 0.75, "Appt & Availability Ctrl", "Scheduling & Conflict Defense", bg="#FFFFFF", border="#64748B")
    draw_box(ax, 3.7, 1.5, 2.5, 0.75, "Consultation & AI Ctrl", "Drawer Rx & AI Summarizer", bg="#FFFFFF", border="#64748B")
    draw_box(ax, 3.7, 0.8, 2.5, 0.55, "Health & OpenAPI Ctrl", "Diagnostics & Swagger UI", bg="#F1F5F9", border="#94A3B8", title_size=8.5)

    # 3. Service Layer & Persistence (Center-Right & Right)
    ax.add_patch(FancyBboxPatch((6.8, 2.4), 2.8, 4.4, boxstyle="round,pad=0.08,rounding_size=0.04", facecolor="#F0FDF4", edgecolor="#16A34A", lw=1.2, ls="--", zorder=1))
    ax.text(8.2, 6.55, "SERVICE & BUSINESS LOGIC", ha="center", va="center", fontsize=9.5, weight="bold", color="#15803D")

    draw_box(ax, 7.0, 5.5, 2.4, 0.75, "Auth & Verification Service", "BCrypt Hashing & SHA-256 Token", bg="#FFFFFF", border="#16A34A")
    draw_box(ax, 7.0, 4.5, 2.4, 0.75, "Appointment Service", "Pessimistic Locks & Conflict Engine", bg="#FFFFFF", border="#16A34A")
    draw_box(ax, 7.0, 3.5, 2.4, 0.75, "Consultation Service", "Records, Prescriptions, History", bg="#FFFFFF", border="#16A34A")
    draw_box(ax, 7.0, 2.6, 2.4, 0.75, "Clinical AI Summary Service", "Medical Safeguards & Sanitization", bg="#FFFFFF", border="#16A34A")

    # 4. External Services & Database (Bottom-Right)
    draw_box(ax, 6.8, 1.25, 1.35, 0.95, "PostgreSQL 16", "JPA / Hibernate ORM\nPort 5432 (ACID)", bg="#FEF3C7", border="#D97706", title_size=9, sub_size=7.5)
    draw_box(ax, 8.25, 1.25, 1.35, 0.95, "External Services", "JavaMailSender (SMTP)\nOpenAI-Compatible LLM", bg="#FDF2F8", border="#DB2777", title_size=9, sub_size=7.5)

    # Connections
    draw_arrow(ax, 2.9, 1.35, 3.7, 4.85, "HTTP / REST (JWT)", color="#0284C7", lw=1.4, label_pos=0.45)
    draw_arrow(ax, 4.95, 4.5, 4.95, 4.25, lw=1.2)
    draw_arrow(ax, 6.2, 5.85, 7.0, 5.85, lw=1.2)
    draw_arrow(ax, 6.2, 2.85, 7.0, 4.85, lw=1.2)
    draw_arrow(ax, 6.2, 1.85, 7.0, 3.85, lw=1.2)
    draw_arrow(ax, 8.2, 2.6, 7.5, 2.2, "Spring Data JPA", color="#15803D", label_pos=0.5, label_offset=(-0.1, 0.0))
    draw_arrow(ax, 8.2, 2.6, 8.8, 2.2, "WebClient / SMTP", color="#9D174D", label_pos=0.5, label_offset=(0.1, 0.0))

    plt.tight_layout()
    out_path = os.path.join(OUT_DIR, "fig1_system_architecture.png")
    plt.savefig(out_path, dpi=300)
    plt.close()
    print("Generated:", out_path)

# ==============================================================================
# Diagram 2: Registration, Login & Email Verification Workflow
# ==============================================================================
def generate_diagram_2():
    fig, ax = plt.subplots(figsize=(10, 6.2), dpi=300)
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 7.5)
    ax.axis("off")

    ax.text(5, 7.15, "Figure 7.2: Registration, Login, JWT Authentication & Email Verification Lifecycle", ha="center", va="center", fontsize=11, weight="bold", color=COLOR_PRIMARY)

    # Phase 1: Registration (Top Row)
    draw_box(ax, 0.5, 5.2, 1.8, 1.1, "1. User Registration", "Submits name, email,\npassword, role in UI", bg="#EFF6FF", border="#3B82F6")
    draw_box(ax, 2.7, 5.2, 2.0, 1.1, "2. Password Hashing\n& User Creation", "BCrypt hash password;\nAssign PAT- / DOC- ID", bg="#F8FAFC", border="#64748B")
    draw_box(ax, 5.1, 5.2, 2.2, 1.1, "3. Token Generation\n& Cryptographic Hash", "Generate 32B Base64URL;\nStore SHA-256 hash in DB", bg="#FEF3C7", border="#D97706")
    draw_box(ax, 7.7, 5.2, 1.8, 1.1, "4. SMTP Dispatch", "Email verification URL\nwith 24h validity", bg="#FDF2F8", border="#DB2777")

    draw_arrow(ax, 2.3, 5.75, 2.7, 5.75, lw=1.2)
    draw_arrow(ax, 4.7, 5.75, 5.1, 5.75, lw=1.2)
    draw_arrow(ax, 7.3, 5.75, 7.7, 5.75, lw=1.2)

    # Phase 2: Verification Action (Middle Row)
    draw_box(ax, 7.7, 3.2, 1.8, 1.1, "5. User Clicks Link", "Frontend catches\n?verify-email=<token>", bg="#F0FDF4", border="#16A34A")
    draw_box(ax, 4.9, 3.2, 2.4, 1.1, "6. Hash Verification\n& Single-Use Cleanup", "POST /api/v1/auth/verify-email\nSHA-256 match & delete token", bg="#F0FDF4", border="#16A34A")
    draw_box(ax, 2.3, 3.2, 2.2, 1.1, "7. Account Verified", "Set email_verified = true\nUnlock high-trust actions", bg="#DCFCE7", border="#15803D")

    draw_arrow(ax, 8.6, 5.2, 8.6, 4.3, "User Inbox", color="#DB2777", lw=1.2, label_pos=0.5, label_offset=(0.4, 0))
    draw_arrow(ax, 7.7, 3.75, 7.3, 3.75, lw=1.2)
    draw_arrow(ax, 4.9, 3.75, 4.5, 3.75, lw=1.2)

    # Phase 3: Login & Access Token (Bottom Row)
    draw_box(ax, 0.5, 1.2, 2.0, 1.1, "8. User Login", "Submits credentials\n(Email + Password)", bg="#EFF6FF", border="#3B82F6")
    draw_box(ax, 3.0, 1.2, 2.2, 1.1, "9. Credential Check\n& JWT Generation", "Verify BCrypt match;\nSign HMAC-SHA256 JWT", bg="#F8FAFC", border="#64748B")
    draw_box(ax, 5.6, 1.2, 2.0, 1.1, "10. In-Memory Token", "Client stores token;\nInjects Bearer header", bg="#EFF6FF", border="#3B82F6")
    draw_box(ax, 8.0, 1.2, 1.5, 1.1, "11. Protected API", "RBAC access to\nportal resources", bg="#DCFCE7", border="#15803D")

    draw_arrow(ax, 2.5, 1.75, 3.0, 1.75, lw=1.2)
    draw_arrow(ax, 5.2, 1.75, 5.6, 1.75, lw=1.2)
    draw_arrow(ax, 7.6, 1.75, 8.0, 1.75, lw=1.2)

    # Rate Limiting & Defense callout
    ax.add_patch(FancyBboxPatch((0.5, 2.8), 1.5, 1.6, boxstyle="round,pad=0.05,rounding_size=0.03", facecolor="#FFFBEB", edgecolor="#F59E0B", lw=1.0, zorder=2))
    ax.text(1.25, 3.9, "ANTI-ENUMERATION\n& RATE LIMITS", ha="center", va="center", fontsize=7.5, weight="bold", color="#B45309")
    ax.text(1.25, 3.2, "• 60s cooldown (429)\n• Uniform resend msg\n• Zero token leakage", ha="center", va="center", fontsize=7, color="#78350F")

    plt.tight_layout()
    out_path = os.path.join(OUT_DIR, "fig2_auth_verification_workflow.png")
    plt.savefig(out_path, dpi=300)
    plt.close()
    print("Generated:", out_path)

# ==============================================================================
# Diagram 3: Availability & Appointment Booking Workflow
# ==============================================================================
def generate_diagram_3():
    fig, ax = plt.subplots(figsize=(10, 6.2), dpi=300)
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 7.5)
    ax.axis("off")

    ax.text(5, 7.15, "Figure 7.3: Doctor Availability Publishing & Concurrency-Safe Appointment Booking", ha="center", va="center", fontsize=11, weight="bold", color=COLOR_PRIMARY)

    # Doctor Lane (Top Half)
    ax.add_patch(FancyBboxPatch((0.4, 4.3), 9.2, 2.5, boxstyle="round,pad=0.05,rounding_size=0.03", facecolor="#F0FDF4", edgecolor="#16A34A", lw=1.0, ls=":", zorder=1))
    ax.text(0.7, 6.55, "DOCTOR SCHEDULE WORKFLOW", fontsize=9, weight="bold", color="#15803D")

    draw_box(ax, 0.7, 4.7, 2.3, 1.2, "Doctor Availability Planner", "Selects date & time intervals;\nRequires email_verified == true", bg="#FFFFFF", border="#16A34A")
    draw_box(ax, 3.5, 4.7, 2.5, 1.2, "Overlap & Interval Check", "Validates start < end;\nEnsures future timestamps", bg="#FFFFFF", border="#16A34A")
    draw_box(ax, 6.5, 4.7, 2.8, 1.2, "Publish Active Slot", "Persists DoctorAvailability\n(is_active = true) in PostgreSQL", bg="#DCFCE7", border="#15803D")

    draw_arrow(ax, 3.0, 5.3, 3.5, 5.3, "POST /availability", lw=1.2, label_size=7.5)
    draw_arrow(ax, 6.0, 5.3, 6.5, 5.3, "Valid", lw=1.2, label_size=7.5)

    # Patient Lane (Bottom Half)
    ax.add_patch(FancyBboxPatch((0.4, 0.5), 9.2, 3.5, boxstyle="round,pad=0.05,rounding_size=0.03", facecolor="#EFF6FF", edgecolor="#2563EB", lw=1.0, ls=":", zorder=1))
    ax.text(0.7, 3.75, "PATIENT APPOINTMENT BOOKING & CONCURRENCY ENGINE", fontsize=9, weight="bold", color="#1D4ED8")

    draw_box(ax, 0.7, 1.8, 2.2, 1.4, "1. Discovery & Selection", "Search by specialty/city;\nSelects active doctor slot;\nSpecifies consultation time", bg="#FFFFFF", border="#2563EB")
    draw_box(ax, 3.3, 1.8, 2.8, 1.4, "2. Defensive Pre-conditions", "• Caller email_verified == true\n• Slot within active window\n• Start strictly before End", bg="#FFFFFF", border="#2563EB")
    draw_box(ax, 6.5, 1.8, 2.8, 1.4, "3. Pessimistic Lock & Check", "• Row-lock User (Patient & Doc)\n• SQL overlap range query\n• Zero double-booking guarantee", bg="#FEF3C7", border="#D97706")

    draw_arrow(ax, 2.9, 2.5, 3.3, 2.5, "POST /appointments", lw=1.2, label_size=7.5)
    draw_arrow(ax, 6.1, 2.5, 6.5, 2.5, "Validated", lw=1.2, label_size=7.5)

    # Outcome split
    draw_box(ax, 5.0, 0.7, 2.0, 0.8, "409 CONFLICT", "Returns helpful error if slot taken", bg="#FEE2E2", border="#EF4444", title_color="#B91C1C", title_size=9)
    draw_box(ax, 7.4, 0.7, 2.0, 0.8, "201 BOOKED", "Appointment successfully created", bg="#DCFCE7", border="#15803D", title_color="#15803D", title_size=9)

    draw_arrow(ax, 7.9, 1.8, 6.0, 1.5, "Conflict detected", color="#DC2626", lw=1.2, label_size=7)
    draw_arrow(ax, 7.9, 1.8, 8.4, 1.5, "Slot free", color="#16A34A", lw=1.2, label_size=7)

    # Link from doctor active slot to patient discovery
    draw_arrow(ax, 7.9, 4.7, 1.8, 3.2, "Available Slots Visible in UI", color="#059669", lw=1.2, label_pos=0.5)

    plt.tight_layout()
    out_path = os.path.join(OUT_DIR, "fig3_availability_booking_workflow.png")
    plt.savefig(out_path, dpi=300)
    plt.close()
    print("Generated:", out_path)

# ==============================================================================
# Diagram 4: Clinical Consultation, Prescription & Follow-up Workflow
# ==============================================================================
def generate_diagram_4():
    fig, ax = plt.subplots(figsize=(10, 6.2), dpi=300)
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 7.5)
    ax.axis("off")

    ax.text(5, 7.15, "Figure 7.4: Clinical Consultation Documentation, Digital Prescriptions & Timeline Lifecycle", ha="center", va="center", fontsize=11, weight="bold", color=COLOR_PRIMARY)

    draw_box(ax, 0.5, 4.8, 2.5, 1.6, "1. Attending Queue", "Doctor reviews upcoming appointments;\nSelects patient in BOOKED state;\nInitiates consultation in portal", bg="#F8FAFC", border="#475569")
    draw_box(ax, 3.5, 4.8, 3.0, 1.6, "2. Slide-over Clinical Drawer", "Records clinical notes:\n• Symptoms & observations\n• Medical diagnosis\n• Treatment advice & follow-up date", bg="#EFF6FF", border="#2563EB")
    draw_box(ax, 7.0, 4.8, 2.5, 1.6, "3. Digital Prescriptions", "Structured prescription items:\n• Medication name\n• Dosage (e.g. 500mg)\n• Frequency & Duration", bg="#EFF6FF", border="#2563EB")

    draw_arrow(ax, 3.0, 5.6, 3.5, 5.6, "Open Drawer", lw=1.2, label_size=7.5)
    draw_arrow(ax, 6.5, 5.6, 7.0, 5.6, "Add Rx", lw=1.2, label_size=7.5)

    # Transition Action
    draw_box(ax, 3.5, 2.6, 3.0, 1.3, "4. Submit Consultation\n(POST /api/v1/consultations)", "Atomic JPA transaction:\n• Persists ConsultationRecord\n• Persists Prescription entities\n• Transitions status: BOOKED → COMPLETED", bg="#FEF3C7", border="#D97706")

    draw_arrow(ax, 8.25, 4.8, 5.0, 3.9, "Doctor Submits", color="#D97706", lw=1.3, label_pos=0.4)

    # Outcomes
    draw_box(ax, 0.8, 0.8, 3.8, 1.2, "Patient Care Timeline", "Patient accesses permanent digital record:\nFull diagnosis, prescribed medicines, follow-up alerts", bg="#DCFCE7", border="#15803D")
    draw_box(ax, 5.4, 0.8, 3.8, 1.2, "Doctor Historical Records", "Archived consultation record linked to doctor profile;\nAvailable for subsequent consultations and AI review", bg="#DCFCE7", border="#15803D")

    draw_arrow(ax, 4.5, 2.6, 2.7, 2.0, "Patient View", color="#15803D", lw=1.2)
    draw_arrow(ax, 5.5, 2.6, 7.3, 2.0, "Doctor View", color="#15803D", lw=1.2)

    plt.tight_layout()
    out_path = os.path.join(OUT_DIR, "fig4_consultation_prescription_workflow.png")
    plt.savefig(out_path, dpi=300)
    plt.close()
    print("Generated:", out_path)

# ==============================================================================
# Diagram 5: AI-Assisted Clinical History Summarizer Workflow
# ==============================================================================
def generate_diagram_5():
    fig, ax = plt.subplots(figsize=(10, 6.2), dpi=300)
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 7.5)
    ax.axis("off")

    ax.text(5, 7.15, "Figure 7.5: AI-Assisted Clinical History Summarizer Workflow & Multi-Layer Medical Safeguards", ha="center", va="center", fontsize=11, weight="bold", color=COLOR_PRIMARY)

    draw_box(ax, 0.5, 5.0, 2.2, 1.4, "1. Doctor Request", "Doctor viewing attending queue\nrequests patient clinical summary;\nRestricted to ROLE_DOCTOR", bg="#F8FAFC", border="#475569")
    draw_box(ax, 3.1, 5.0, 2.4, 1.4, "2. Clinical Disclosure", "Doctor must confirm explicit\ndisclosure acknowledgement\nbefore request is transmitted", bg="#FEF3C7", border="#D97706")
    draw_box(ax, 5.9, 5.0, 3.6, 1.4, "3. Data Sanitization & Token Budget", "• Strips internal user IDs, keys, emails\n• Formats chronological visits & Rx\n• Enforces max 6000 character budget", bg="#EFF6FF", border="#2563EB")

    draw_arrow(ax, 2.7, 5.7, 3.1, 5.7, lw=1.2)
    draw_arrow(ax, 5.5, 5.7, 5.9, 5.7, "Acknowledged", lw=1.2, label_size=7.5)

    # AI Execution
    draw_box(ax, 5.9, 2.8, 3.6, 1.4, "4. Strict Medical Safety Prompt", "System prompt enforces:\n• Ground truth only in records\n• NO diagnosis inference\n• NO dosage alterations\n• Exact 6-section structure", bg="#FDF2F8", border="#DB2777")
    draw_box(ax, 1.5, 2.8, 3.6, 1.4, "5. LLM Client Execution", "OpenAiCompatibleClinicalSummaryClient\ndispatches request to configured endpoint;\nConfigurable timeouts & mock fallbacks", bg="#F1F5F9", border="#475569")

    draw_arrow(ax, 7.7, 5.0, 7.7, 4.2, lw=1.2)
    draw_arrow(ax, 5.9, 3.5, 5.1, 3.5, "HTTP POST", lw=1.2)

    # Delivery & Review
    draw_box(ax, 1.5, 0.7, 7.0, 1.4, "6. Clinical Draft Presentation & Doctor-in-the-Loop", "• Displayed strictly as a reviewable clinical draft with prominent safety disclaimer\n• Doctor reviews, edits, and incorporates verified excerpts into consultation notes\n• Zero automated diagnostic decisions; final medical responsibility remains with physician", bg="#DCFCE7", border="#15803D")

    draw_arrow(ax, 3.3, 2.8, 3.3, 2.1, "Draft Summary Returned", color="#15803D", lw=1.2, label_pos=0.5, label_offset=(0.8, 0))

    plt.tight_layout()
    out_path = os.path.join(OUT_DIR, "fig5_ai_summary_workflow.png")
    plt.savefig(out_path, dpi=300)
    plt.close()
    print("Generated:", out_path)

# ==============================================================================
# Diagram 6: Entity-Relationship Diagram (ERD)
# ==============================================================================
def generate_diagram_6():
    fig, ax = plt.subplots(figsize=(10, 6.5), dpi=300)
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 7.5)
    ax.axis("off")

    ax.text(5, 7.25, "Figure 8.1: Complete Relational Database Entity-Relationship Diagram (ERD)", ha="center", va="center", fontsize=11, weight="bold", color=COLOR_PRIMARY)

    # 1. users
    draw_box(ax, 0.4, 3.8, 2.6, 3.0, "users", 
             "id : BIGINT (PK)\npublic_id : VARCHAR (UK)\nfull_name : VARCHAR\nemail : VARCHAR (UK)\npassword : VARCHAR (BCrypt)\nrole : VARCHAR (ENUM)\nphone_number : VARCHAR\nemail_verified : BOOLEAN",
             bg="#EFF6FF", border="#1E3A8A", title_color="#1E3A8A", title_size=10, sub_size=7)

    # 2. doctor_profiles
    draw_box(ax, 3.7, 5.2, 2.8, 1.8, "doctor_profiles",
             "id : BIGINT (PK)\ndoctor_user_id : BIGINT (FK, UK)\nspecialty, clinic_name : VARCHAR\nclinic_address, city, area : VARCHAR\npin_code, clinic_phone : VARCHAR\npersonal_phone : VARCHAR",
             bg="#F0FDF4", border="#16A34A", title_color="#15803D", title_size=9.5, sub_size=7)

    # 3. email_verification_tokens
    draw_box(ax, 0.4, 0.8, 2.6, 1.8, "email_verification_tokens",
             "id : BIGINT (PK)\nuser_id : BIGINT (FK, UK)\ntoken_hash : VARCHAR(64)\ncreated_at : TIMESTAMP\nexpires_at : TIMESTAMP\nlast_sent_at : TIMESTAMP",
             bg="#FEF3C7", border="#D97706", title_color="#B45309", title_size=9.5, sub_size=7)

    # 4. doctor_availability
    draw_box(ax, 3.7, 2.8, 2.8, 1.7, "doctor_availability",
             "id : BIGINT (PK)\ndoctor_id : BIGINT (FK)\nstart_time : TIMESTAMP\nend_time : TIMESTAMP\nis_active : BOOLEAN\ncreated_at : TIMESTAMP",
             bg="#F0FDF4", border="#16A34A", title_color="#15803D", title_size=9.5, sub_size=7)

    # 5. appointments
    draw_box(ax, 7.1, 3.6, 2.5, 2.2, "appointments",
             "id : BIGINT (PK)\npatient_id : BIGINT (FK)\ndoctor_id : BIGINT (FK)\navailability_id : BIGINT (FK)\nstart_time, end_time : TIMESTAMP\nstatus : VARCHAR (ENUM)\ncreated_at, updated_at : TIMESTAMP",
             bg="#EFF6FF", border="#1E3A8A", title_color="#1E3A8A", title_size=9.5, sub_size=7)

    # 6. consultation_records
    draw_box(ax, 7.1, 1.3, 2.5, 1.9, "consultation_records",
             "id : BIGINT (PK)\nappointment_id : BIGINT (FK, UK)\ndoctor_id, patient_id : BIGINT (FK)\nsymptoms, diagnosis : TEXT\ntreatment_advice : TEXT\nfollow_up_date : DATE",
             bg="#FDF2F8", border="#DB2777", title_color="#9D174D", title_size=9.5, sub_size=7)

    # 7. prescriptions
    draw_box(ax, 3.7, 0.6, 2.8, 1.6, "prescriptions",
             "id : BIGINT (PK)\nconsultation_record_id : BIGINT (FK)\nmedication_name : VARCHAR\ndosage, frequency : VARCHAR\nduration : VARCHAR",
             bg="#FDF2F8", border="#DB2777", title_color="#9D174D", title_size=9.5, sub_size=7)

    # ER Relations
    draw_arrow(ax, 3.0, 5.8, 3.7, 5.8, "1 : 0..1", lw=1.2, label_size=7.5)
    draw_arrow(ax, 1.7, 3.8, 1.7, 2.6, "1 : 0..1", lw=1.2, label_size=7.5)
    draw_arrow(ax, 3.0, 4.4, 3.7, 3.8, "1 : 0..N (Doctor)", lw=1.2, label_size=7)
    draw_arrow(ax, 6.5, 3.6, 7.1, 4.2, "1 : 0..N", lw=1.2, label_size=7.5)
    draw_arrow(ax, 3.0, 5.2, 7.1, 5.2, "1 : 0..N (Doctor/Patient)", lw=1.2, label_size=7)
    draw_arrow(ax, 8.35, 3.6, 8.35, 3.2, "1 : 1", lw=1.2, label_size=7.5)
    draw_arrow(ax, 7.1, 1.9, 6.5, 1.6, "1 : 0..N", lw=1.2, label_size=7.5)

    plt.tight_layout()
    out_path = os.path.join(OUT_DIR, "fig6_entity_relationship_diagram.png")
    plt.savefig(out_path, dpi=300)
    plt.close()
    print("Generated:", out_path)

# ==============================================================================
# Diagram 7: Role-Based Access Control (RBAC) Matrix
# ==============================================================================
def generate_diagram_7():
    fig, ax = plt.subplots(figsize=(10, 6.2), dpi=300)
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 7.5)
    ax.axis("off")

    ax.text(5, 7.15, "Figure 9.1: Role-Based Access Control (RBAC) Security Architecture & Privacy Boundaries", ha="center", va="center", fontsize=11, weight="bold", color=COLOR_PRIMARY)

    # Left Column: Public Unauthenticated
    draw_box(ax, 0.4, 1.2, 2.7, 5.4, "PUBLIC ACCESS\n(Unauthenticated)", 
             "• User Registration\n  (PATIENT / DOCTOR)\n• User Login (JWT generation)\n• Email Verification Token Submit\n• Public Doctor Search\n  (Specialty, City, Area, PIN)\n• View Doctor Public Clinic Profile\n  (Clinic Phone & Address)\n• Swagger UI & Health Diagnostics\n\n[STRICTLY PROHIBITED]\n✗ View Private Phone Numbers\n✗ Book Appointments\n✗ View Queues or Calendars",
             bg="#F8FAFC", border="#64748B", title_color="#334155", title_size=9.5, sub_size=7.5)

    # Center Column: ROLE_PATIENT
    draw_box(ax, 3.65, 1.2, 2.7, 5.4, "ROLE_PATIENT\n(Authenticated Patient)",
             "• Manage Private Contact Profile\n  (Private phone_number)\n• Search Doctors & Browse Slots\n• Book Appointment\n  (Requires email_verified = true)\n• Cancel Own Appointment\n• View Personal Care Timeline\n• View Prescriptions & Advice\n• Resend Verification Email\n\n[STRICTLY PROHIBITED]\n✗ Access Doctor Calendars\n✗ View Attending Patient Queue\n✗ Create Availability Slots\n✗ Generate AI Summaries",
             bg="#EFF6FF", border="#2563EB", title_color="#1D4ED8", title_size=9.5, sub_size=7.5)

    # Right Column: ROLE_DOCTOR
    draw_box(ax, 6.9, 1.2, 2.7, 5.4, "ROLE_DOCTOR\n(Authenticated Doctor)",
             "• Manage Doctor Profile\n  (Clinic & Personal Phone)\n• Calendar Availability Planner\n  (Requires email_verified = true)\n• Deactivate Unbooked Slots\n• View Attending Patient Queue\n• Open Clinical Consultation Drawer\n• Document Diagnoses & Prescriptions\n• Generate AI Clinical Summaries\n  (With Disclosure Confirmation)\n• Complete Consultations\n\n[STRICTLY PROHIBITED]\n✗ Book Patient Appointments\n✗ Alter Other Doctors' Schedules",
             bg="#F0FDF4", border="#16A34A", title_color="#15803D", title_size=9.5, sub_size=7.5)

    # Top Gateway banner
    draw_box(ax, 0.4, 6.0, 9.2, 0.7, "Spring Security 6 Gateway Enforces JWT Stateless Bearer Validation & @PreAuthorize Method Rules", bg="#EEF2FF", border="#4F46E5", title_color="#4338CA", title_size=9.5)

    plt.tight_layout()
    out_path = os.path.join(OUT_DIR, "fig7_rbac_workflow.png")
    plt.savefig(out_path, dpi=300)
    plt.close()
    print("Generated:", out_path)

if __name__ == "__main__":
    generate_diagram_1()
    generate_diagram_2()
    generate_diagram_3()
    generate_diagram_4()
    generate_diagram_5()
    generate_diagram_6()
    generate_diagram_7()
    print("All 7 publication-quality academic diagrams generated successfully!")
