from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.units import cm
from reportlab.pdfgen import canvas
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import Paragraph
from reportlab.lib.enums import TA_CENTER
from datetime import datetime


# ── Brand Colors (matching your PIDEV project) ────────────────────────────────
NAVY       = colors.HexColor("#1A2B4A")
GOLD       = colors.HexColor("#C9A84C")
LIGHT_GRAY = colors.HexColor("#F5F5F5")
DARK_GRAY  = colors.HexColor("#4A4A4A")
WHITE      = colors.white


def generate_certificate(
    output_path: str,
    certificate_id: str,
    student_name: str,
    student_email: str,
    exam_title: str,
    score: float,
    max_score: float,
    passed_at: str,
):
    """Generate a professional PDF certificate using ReportLab."""
    width, height = A4
    c = canvas.Canvas(output_path, pagesize=A4)

    _draw_background(c, width, height)
    _draw_border(c, width, height)
    _draw_header(c, width, height)
    _draw_body(c, width, height, student_name, exam_title, score, max_score, passed_at)
    _draw_footer(c, width, height, certificate_id, student_email)

    c.save()


def _draw_background(c, width, height):
    """Navy gradient-like background."""
    c.setFillColor(NAVY)
    c.rect(0, 0, width, height, fill=1, stroke=0)

    # Light inner background
    margin = 1.2 * cm
    c.setFillColor(WHITE)
    c.roundRect(margin, margin, width - 2 * margin, height - 2 * margin, 12, fill=1, stroke=0)


def _draw_border(c, width, height):
    """Gold decorative border."""
    margin = 1.2 * cm
    inner = 0.4 * cm

    # Outer gold border
    c.setStrokeColor(GOLD)
    c.setLineWidth(3)
    c.roundRect(margin, margin, width - 2 * margin, height - 2 * margin, 12, fill=0, stroke=1)

    # Inner thin gold border
    c.setLineWidth(1)
    c.roundRect(
        margin + inner, margin + inner,
        width - 2 * (margin + inner), height - 2 * (margin + inner),
        8, fill=0, stroke=1
    )


def _draw_header(c, width, height):
    """Draw school logo area and certificate title."""
    # Top navy banner
    banner_h = 4.5 * cm
    banner_y = height - 1.2 * cm - banner_h
    c.setFillColor(NAVY)
    c.roundRect(1.2 * cm, banner_y, width - 2.4 * cm, banner_h, 10, fill=1, stroke=0)

    # School name
    c.setFillColor(WHITE)
    c.setFont("Helvetica-Bold", 22)
    c.drawCentredString(width / 2, height - 3.0 * cm, "ENGLISH SCHOOL MANAGEMENT")

    # Subtitle
    c.setFillColor(GOLD)
    c.setFont("Helvetica", 11)
    c.drawCentredString(width / 2, height - 3.8 * cm, "ESPRIT SCHOOL OF ENGINEERING  •  PIDEV 2025–2026")

    # Gold separator line
    c.setStrokeColor(GOLD)
    c.setLineWidth(2)
    c.line(3 * cm, height - 5.8 * cm, width - 3 * cm, height - 5.8 * cm)

    # "CERTIFICATE OF ACHIEVEMENT"
    c.setFillColor(NAVY)
    c.setFont("Helvetica-Bold", 16)
    c.drawCentredString(width / 2, height - 7.2 * cm, "CERTIFICATE OF ACHIEVEMENT")


def _draw_body(c, width, height, student_name, exam_title, score, max_score, passed_at):
    """Draw the main certificate body."""
    # "This is to certify that"
    c.setFillColor(DARK_GRAY)
    c.setFont("Helvetica-Oblique", 12)
    c.drawCentredString(width / 2, height - 9.0 * cm, "This is to certify that")

    # Student name  (large gold)
    c.setFillColor(NAVY)
    c.setFont("Helvetica-Bold", 30)
    c.drawCentredString(width / 2, height - 11.0 * cm, student_name)

    # Gold underline
    name_w = c.stringWidth(student_name, "Helvetica-Bold", 30)
    c.setStrokeColor(GOLD)
    c.setLineWidth(1.5)
    c.line(width / 2 - name_w / 2, height - 11.4 * cm,
           width / 2 + name_w / 2, height - 11.4 * cm)

    # "has successfully completed"
    c.setFillColor(DARK_GRAY)
    c.setFont("Helvetica-Oblique", 12)
    c.drawCentredString(width / 2, height - 12.5 * cm, "has successfully completed and passed the examination")

    # Exam title
    c.setFillColor(NAVY)
    c.setFont("Helvetica-Bold", 18)
    c.drawCentredString(width / 2, height - 14.0 * cm, f'"{exam_title}"')

    # Score badge
    percentage = round((score / max_score) * 100, 1) if max_score > 0 else 0
    badge_cx = width / 2
    badge_cy = height - 16.5 * cm

    c.setFillColor(GOLD)
    c.circle(badge_cx, badge_cy, 1.5 * cm, fill=1, stroke=0)
    c.setFillColor(WHITE)
    c.setFont("Helvetica-Bold", 16)
    c.drawCentredString(badge_cx, badge_cy + 0.15 * cm, f"{percentage}%")
    c.setFont("Helvetica", 8)
    c.drawCentredString(badge_cx, badge_cy - 0.55 * cm, "SCORE")

    # Score detail
    c.setFillColor(DARK_GRAY)
    c.setFont("Helvetica", 11)
    c.drawCentredString(width / 2, height - 18.5 * cm, f"Score obtained: {score} / {max_score} points")

    # Date
    try:
        dt = datetime.fromisoformat(passed_at.replace("Z", "+00:00"))
        date_str = dt.strftime("%B %d, %Y")
    except Exception:
        date_str = passed_at

    c.setFont("Helvetica", 11)
    c.drawCentredString(width / 2, height - 19.5 * cm, f"Completed on: {date_str}")


def _draw_footer(c, width, height, certificate_id, student_email):
    """Draw signature lines and certificate ID."""
    footer_y = 4.5 * cm

    # Signature lines
    line_w = 5 * cm
    c.setStrokeColor(NAVY)
    c.setLineWidth(1)

    left_x  = width / 2 - 7 * cm
    right_x = width / 2 + 2 * cm

    c.line(left_x, footer_y, left_x + line_w, footer_y)
    c.line(right_x, footer_y, right_x + line_w, footer_y)

    c.setFillColor(DARK_GRAY)
    c.setFont("Helvetica", 9)
    c.drawCentredString(left_x + line_w / 2, footer_y - 0.5 * cm, "Supervisor")
    c.drawCentredString(left_x + line_w / 2, footer_y - 0.9 * cm, "Nadine Mili")
    c.drawCentredString(right_x + line_w / 2, footer_y - 0.5 * cm, "Director")
    c.drawCentredString(right_x + line_w / 2, footer_y - 0.9 * cm, "Esprit School")

    # Certificate ID & email
    c.setFillColor(colors.HexColor("#999999"))
    c.setFont("Helvetica", 7)
    c.drawCentredString(width / 2, 2.2 * cm, f"Certificate ID: {certificate_id}")
    c.drawCentredString(width / 2, 1.7 * cm, f"Issued to: {student_email}  |  Verify at: http://localhost:8097/api/certificates/download/{certificate_id}")
