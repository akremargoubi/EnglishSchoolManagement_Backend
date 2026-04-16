from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse
from pydantic import BaseModel
from datetime import datetime
import uuid
import os

from certificate_generator import generate_certificate

app = FastAPI(
    title="Certificate Service",
    description="Microservice de génération de certificats PDF pour English School Management",
    version="1.0.0"
)

CERTIFICATES_DIR = "certificates"
os.makedirs(CERTIFICATES_DIR, exist_ok=True)


# ── Models ────────────────────────────────────────────────────────────────────

class CertificateRequest(BaseModel):
    studentName: str
    studentEmail: str
    examTitle: str
    score: float
    maxScore: float
    passedAt: str  # ISO date string from Assessment Service


class CertificateResponse(BaseModel):
    certificateId: str
    studentName: str
    examTitle: str
    score: float
    downloadUrl: str
    generatedAt: str


# ── Endpoints ─────────────────────────────────────────────────────────────────

@app.get("/health")
def health_check():
    return {"status": "UP", "service": "certificate-service", "port": 8097}


@app.post("/api/certificates/generate", response_model=CertificateResponse)
def generate(req: CertificateRequest):
    """
    Called by Assessment Service when a student passes an exam.
    Generates a PDF certificate and returns its download URL.
    """
    certificate_id = str(uuid.uuid4())
    filename = f"{certificate_id}.pdf"
    filepath = os.path.join(CERTIFICATES_DIR, filename)

    try:
        generate_certificate(
            output_path=filepath,
            certificate_id=certificate_id,
            student_name=req.studentName,
            student_email=req.studentEmail,
            exam_title=req.examTitle,
            score=req.score,
            max_score=req.maxScore,
            passed_at=req.passedAt,
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Certificate generation failed: {str(e)}")

    return CertificateResponse(
        certificateId=certificate_id,
        studentName=req.studentName,
        examTitle=req.examTitle,
        score=req.score,
        downloadUrl=f"/api/certificates/download/{certificate_id}",
        generatedAt=datetime.utcnow().isoformat(),
    )


@app.get("/api/certificates/download/{certificate_id}")
def download(certificate_id: str):
    """Download a generated certificate PDF by its ID."""
    filepath = os.path.join(CERTIFICATES_DIR, f"{certificate_id}.pdf")
    if not os.path.exists(filepath):
        raise HTTPException(status_code=404, detail="Certificate not found")
    return FileResponse(
        path=filepath,
        media_type="application/pdf",
        filename=f"certificate_{certificate_id}.pdf"
    )


@app.delete("/api/certificates/{certificate_id}")
def delete_certificate(certificate_id: str):
    """Delete a certificate file."""
    filepath = os.path.join(CERTIFICATES_DIR, f"{certificate_id}.pdf")
    if not os.path.exists(filepath):
        raise HTTPException(status_code=404, detail="Certificate not found")
    os.remove(filepath)
    return {"message": f"Certificate {certificate_id} deleted successfully"}
