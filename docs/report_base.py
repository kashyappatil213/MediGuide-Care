"""
MediGuide Care — Final MCA Mini-Project Academic Report Generator
Generates a complete, professionally formatted academic report as a PDF in docs/
Strict adherence to academic standards:
- Font: Times New Roman throughout
- Margins: Exactly 1 inch (72 pt) all around
- Line spacing: 1.5 (12 pt font with 18 pt leading)
- Chapter Headings: 14 pt Bold
- Subheadings: 12-13 pt Bold
- Clickable Table of Contents, List of Figures, and List of Tables
- Preliminary pages numbered with lower-case Roman numerals (ii, iii, iv...)
- Main body numbered with Arabic numerals (1, 2, 3...) starting from Chapter 1
- High-resolution embedded vector/PNG diagrams from docs/diagrams/
- Verifiable technical data matching MediGuide Care implementation
"""

import os
import io
import pypdf
from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.units import inch
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, PageBreak, Spacer, Table, TableStyle, Image, KeepTogether, Flowable
)
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

# ------------------------------------------------------------------------------
# 1. Register Times New Roman TrueType Fonts
# ------------------------------------------------------------------------------
pdfmetrics.registerFont(TTFont('TimesNewRoman', 'C:/Windows/Fonts/times.ttf'))
pdfmetrics.registerFont(TTFont('TimesNewRoman-Bold', 'C:/Windows/Fonts/timesbd.ttf'))
pdfmetrics.registerFont(TTFont('TimesNewRoman-Italic', 'C:/Windows/Fonts/timesi.ttf'))
pdfmetrics.registerFont(TTFont('TimesNewRoman-BoldItalic', 'C:/Windows/Fonts/timesbi.ttf'))
pdfmetrics.registerFontFamily(
    'TimesNewRoman',
    normal='TimesNewRoman',
    bold='TimesNewRoman-Bold',
    italic='TimesNewRoman-Italic',
    boldItalic='TimesNewRoman-BoldItalic'
)

# ------------------------------------------------------------------------------
# 2. Page & Color Geometry
# ------------------------------------------------------------------------------
PAGE_WIDTH, PAGE_HEIGHT = A4
MARGIN = 72.0  # Exactly 1 inch
USABLE_WIDTH = PAGE_WIDTH - 2 * MARGIN
USABLE_HEIGHT = PAGE_HEIGHT - 2 * MARGIN

COLOR_PRIMARY = colors.HexColor("#1E3A8A")     # Academic Navy
COLOR_SECONDARY = colors.HexColor("#0F766E")   # Academic Teal
COLOR_TEXT = colors.HexColor("#0F172A")        # Deep Slate Body Text
COLOR_MUTED = colors.HexColor("#475569")       # Slate Muted
COLOR_LIGHT_BG = colors.HexColor("#F8FAFC")    # Table Alternate Row
COLOR_BORDER = colors.HexColor("#CBD5E1")      # Table Borders
COLOR_CODE_BG = colors.HexColor("#F1F5F9")     # Code Box Fill
COLOR_LINK = colors.HexColor("#1E3A8A")        # Clickable Hyperlink Color

# ------------------------------------------------------------------------------
# 3. Custom Flowable: TrackedBookmark
# ------------------------------------------------------------------------------
class TrackedBookmark(Flowable):
    """
    Creates an internal PDF named destination and records its page number into tracker.
    """
    def __init__(self, key, tracker=None):
        super().__init__()
        self.key = key
        self.tracker = tracker
        self.width = 0
        self.height = 0

    def draw(self):
        self.canv.bookmarkPage(self.key)
        if self.tracker is not None:
            self.tracker[self.key] = self.canv.getPageNumber()

# ------------------------------------------------------------------------------
# 4. Custom Numbered Academic Canvas
# ------------------------------------------------------------------------------
class AcademicNumberedCanvas(canvas.Canvas):
    """
    Two-pass canvas that applies academic page numbering, running headers, and footers.
    - Page 1 (Title): No header, no footer, no page number.
    - Prelim pages (2 .. prelim_end_page): Roman numerals (ii, iii, iv...) centered at bottom.
    - Main body (prelim_end_page + 1 .. end): Arabic numerals (1, 2, 3...) starting at 1.
    """
    prelim_end_page = 8  # Will be dynamically set based on first pass

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for page_idx, state in enumerate(self._saved_page_states, 1):
            self.__dict__.update(state)
            self.draw_academic_decorations(page_idx, num_pages)
            super().showPage()
        super().save()

    def draw_academic_decorations(self, page_num, total_pages):
        self.saveState()
        self.setFont("TimesNewRoman", 10)
        self.setStrokeColor(COLOR_BORDER)
        self.setLineWidth(0.6)

        # 1. Title Page (suppress everything)
        if page_num == 1:
            self.restoreState()
            return

        # 2. Preliminary Pages (lower-case Roman numerals)
        if page_num <= self.prelim_end_page:
            romans = ["", "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x", "xi", "xii", "xiii"]
            roman_str = romans[page_num] if page_num < len(romans) else str(page_num)
            self.drawCentredString(PAGE_WIDTH / 2.0, 45, roman_str)
            self.restoreState()
            return

        # 3. Main Body Pages (Arabic numerals starting from 1)
        main_page = page_num - self.prelim_end_page

        # Running Header (Omit on first page of new chapters if needed, or uniform clean header)
        self.setFillColor(COLOR_MUTED)
        self.drawString(MARGIN, PAGE_HEIGHT - 50, "MediGuide Care — Final MCA Project Report")
        self.drawRightString(PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 50, "Department of Computer Applications")
        self.line(MARGIN, PAGE_HEIGHT - 55, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 55)

        # Running Footer
        self.drawString(MARGIN, 45, "Doctor–Patient Consultation & Follow-up System")
        self.drawRightString(PAGE_WIDTH - MARGIN, 45, f"Page {main_page}")
        self.line(MARGIN, 58, PAGE_WIDTH - MARGIN, 58)

        self.restoreState()

# ------------------------------------------------------------------------------
# 5. Typography Styles Definition
# ------------------------------------------------------------------------------
def setup_styles():
    styles = getSampleStyleSheet()

    # Normal Body Text: Times New Roman, 12 pt, 18 pt leading (1.5 line spacing), Justified
    body = ParagraphStyle(
        'AcademicBody',
        parent=styles['Normal'],
        fontName='TimesNewRoman',
        fontSize=12,
        leading=18,
        alignment=4,  # Justified
        textColor=COLOR_TEXT,
        spaceAfter=8,
        spaceBefore=0
    )

    # Bullet / List item
    bullet = ParagraphStyle(
        'AcademicBullet',
        parent=body,
        leftIndent=20,
        bulletIndent=8,
        spaceAfter=4,
        spaceBefore=0
    )

    # Chapter Heading: 14 pt, Bold, Centered/Left, 20 pt leading
    chapter_heading = ParagraphStyle(
        'AcademicChapterHeading',
        parent=styles['Heading1'],
        fontName='TimesNewRoman-Bold',
        fontSize=14,
        leading=20,
        alignment=0,  # Left
        textColor=COLOR_PRIMARY,
        spaceBefore=14,
        spaceAfter=10,
        keepWithNext=True
    )

    # Section Heading (H1): 13 pt, Bold, 18 pt leading
    h1 = ParagraphStyle(
        'AcademicH1',
        parent=styles['Heading2'],
        fontName='TimesNewRoman-Bold',
        fontSize=13,
        leading=18,
        alignment=0,
        textColor=COLOR_PRIMARY,
        spaceBefore=12,
        spaceAfter=6,
        keepWithNext=True
    )

    # Subsection Heading (H2): 12 pt, Bold, 16 pt leading
    h2 = ParagraphStyle(
        'AcademicH2',
        parent=styles['Heading3'],
        fontName='TimesNewRoman-Bold',
        fontSize=12,
        leading=16,
        alignment=0,
        textColor=COLOR_SECONDARY,
        spaceBefore=10,
        spaceAfter=4,
        keepWithNext=True
    )

    # Caption (Figures and Tables): 11 pt, Italic, Centered
    caption = ParagraphStyle(
        'AcademicCaption',
        parent=styles['Normal'],
        fontName='TimesNewRoman-Italic',
        fontSize=11,
        leading=15,
        alignment=1,  # Centered
        textColor=COLOR_MUTED,
        spaceBefore=6,
        spaceAfter=12
    )

    # Code / Monospace Style: Courier, 9.5 pt, 13 pt leading
    code_style = ParagraphStyle(
        'AcademicCode',
        parent=styles['Code'],
        fontName='Courier',
        fontSize=9.5,
        leading=13,
        textColor=colors.HexColor("#0F172A")
    )

    # Table Text (Cell paragraphs)
    table_cell = ParagraphStyle(
        'AcademicTableCell',
        parent=styles['Normal'],
        fontName='TimesNewRoman',
        fontSize=10,
        leading=14,
        textColor=COLOR_TEXT
    )

    table_header = ParagraphStyle(
        'AcademicTableHeader',
        parent=styles['Normal'],
        fontName='TimesNewRoman-Bold',
        fontSize=10,
        leading=14,
        textColor=colors.white,
        alignment=0
    )

    # TOC Entry Style
    toc_entry = ParagraphStyle(
        'AcademicTOCEntry',
        parent=styles['Normal'],
        fontName='TimesNewRoman',
        fontSize=11,
        leading=16,
        textColor=COLOR_TEXT
    )

    toc_entry_bold = ParagraphStyle(
        'AcademicTOCEntryBold',
        parent=styles['Normal'],
        fontName='TimesNewRoman-Bold',
        fontSize=11,
        leading=17,
        textColor=COLOR_PRIMARY
    )

    return {
        'body': body,
        'bullet': bullet,
        'chapter': chapter_heading,
        'h1': h1,
        'h2': h2,
        'caption': caption,
        'code': code_style,
        'table_cell': table_cell,
        'table_header': table_header,
        'toc_entry': toc_entry,
        'toc_bold': toc_entry_bold
    }

# ------------------------------------------------------------------------------
# 6. Helper Flowable Functions
# ------------------------------------------------------------------------------
def create_code_box(code_text, styles):
    """Encapsulates code or API snippet in a clean shaded box."""
    p = Paragraph(code_text.replace("\n", "<br/>").replace(" ", "&nbsp;"), styles['code'])
    t = Table([[p]], colWidths=[USABLE_WIDTH])
    t.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), COLOR_CODE_BG),
        ('BOX', (0, 0), (-1, -1), 0.8, COLOR_BORDER),
        ('PADDING', (0, 0), (-1, -1), 8),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
    ]))
    return t

def format_toc_line(title, page_str, target_key, styles, is_bold=False, indent=0):
    """
    Renders a clickable Table of Contents line with dot leader and page number.
    e.g. Chapter 1: Introduction ............................................. 1
    """
    style = styles['toc_bold'] if is_bold else styles['toc_entry']
    color_hex = "#1E3A8A" if is_bold else "#0F172A"
    
    # We construct a 2-column table: Left has title and dotted leader, Right has page number
    dots = ". " * 50
    left_text = f'<a href="#{target_key}" color="{color_hex}">{title}</a>'
    right_text = f'<a href="#{target_key}" color="{color_hex}"><b>{page_str}</b></a>' if is_bold else f'<a href="#{target_key}" color="{color_hex}">{page_str}</a>'
    
    # Left width: USABLE_WIDTH - 45 pt, Right width: 45 pt
    col1 = Paragraph(left_text, style)
    col2 = Paragraph(right_text, ParagraphStyle('R', parent=style, alignment=2))
    
    t = Table([[col1, col2]], colWidths=[USABLE_WIDTH - 45 - indent, 45])
    t.setStyle(TableStyle([
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('LEFTPADDING', (0, 0), (-1, -1), 0),
        ('RIGHTPADDING', (0, 0), (-1, -1), 0),
        ('TOPPADDING', (0, 0), (-1, -1), 1.5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 1.5),
    ]))
    
    if indent > 0:
        indent_t = Table([[ "", t ]], colWidths=[indent, USABLE_WIDTH - indent])
        indent_t.setStyle(TableStyle([
            ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
            ('LEFTPADDING', (0, 0), (-1, -1), 0),
            ('RIGHTPADDING', (0, 0), (-1, -1), 0),
            ('TOPPADDING', (0, 0), (-1, -1), 0),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 0),
        ]))
        return indent_t
    return t

def format_academic_table(header_row, data_rows, col_widths, styles):
    """Creates a standardized academic table with styled headers and alternating row shading."""
    table_data = []
    # Header
    h_row = [Paragraph(f"<b>{cell}</b>", styles['table_header']) for cell in header_row]
    table_data.append(h_row)
    
    # Rows
    for r_idx, row in enumerate(data_rows):
        r = [Paragraph(str(cell), styles['table_cell']) for cell in row]
        table_data.append(r)
        
    t = Table(table_data, colWidths=col_widths, repeatRows=1)
    t_style = [
        ('BACKGROUND', (0, 0), (-1, 0), COLOR_PRIMARY),
        ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, COLOR_BORDER),
        ('BOX', (0, 0), (-1, -1), 1.0, COLOR_PRIMARY),
        ('TOPPADDING', (0, 0), (-1, -1), 5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 5),
        ('LEFTPADDING', (0, 0), (-1, -1), 6),
        ('RIGHTPADDING', (0, 0), (-1, -1), 6),
    ]
    # Alternating row background
    for i in range(1, len(table_data)):
        if i % 2 == 0:
            t_style.append(('BACKGROUND', (0, i), (-1, i), COLOR_LIGHT_BG))
    t.setStyle(TableStyle(t_style))
    return t

print("Base setup definitions ready.")
