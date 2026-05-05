import os
import hashlib
from datetime import datetime

import pandas as pd
import psycopg2
import pymysql
from fastapi import APIRouter, HTTPException

from config import PG, MY, DATASET_PATH, FEATURE_COLS, NIVEAU_MAP

router = APIRouter()


# ── helpers ────────────────────────────────────────────────────────────────────

def _pg(cfg: dict):
    return psycopg2.connect(**cfg)

def _my(cfg: dict):
    return pymysql.connect(
        host=cfg["host"], port=cfg["port"], db=cfg["db"],
        user=cfg["user"], password=cfg["password"], charset=cfg["charset"],
    )

def derive_age(email: str) -> int:
    h = int(hashlib.md5(email.encode()).hexdigest()[:8], 16)
    return 18 + (h % 18)

def derive_genre(email: str) -> str:
    h = int(hashlib.md5(email.encode()).hexdigest()[8:16], 16)
    return "M" if h % 2 == 0 else "F"


# ── assembly ───────────────────────────────────────────────────────────────────

def build_dataset() -> pd.DataFrame:
    # Step 1 — students base (auth-db)
    conn = _pg(PG["auth"])
    try:
        students = pd.read_sql("""
            SELECT u.id::text AS etudiant_id, u.email,
                   u.first_name AS prenom, u.last_name AS nom,
                   u.status AS statut,
                   c.level AS niveau, c.name AS class_name
            FROM users u
            LEFT JOIN classes c ON u.class_id = c.id
            WHERE u.role = 'STUDENT'
              AND u.deleted_at IS NULL
        """, conn)
    finally:
        conn.close()

    # Step 2 — attendance metrics (attendance-db)
    conn = _pg(PG["attendance"])
    try:
        attendance = pd.read_sql("""
            SELECT student_email,
                   COUNT(*) AS nb_seances,
                   SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) AS nb_absences
            FROM attendances
            GROUP BY student_email
        """, conn)
    finally:
        conn.close()

    attendance["taux_absence"] = attendance.apply(
        lambda r: float(r["nb_absences"]) / float(r["nb_seances"]) if r["nb_seances"] > 0 else 0.0,
        axis=1,
    )

    # Step 3 — grade metrics (assessment-db MySQL)
    conn = _my(MY["assessment"])
    try:
        grades = pd.read_sql("""
            SELECT g.student_email,
                   AVG(g.score / NULLIF(g.max_score, 0) * 20) AS note_moyenne,
                   MIN(g.score / NULLIF(g.max_score, 0) * 20) AS note_min,
                   MAX(g.score / NULLIF(g.max_score, 0) * 20) AS note_max,
                   STDDEV(g.score / NULLIF(g.max_score, 0) * 20) AS note_std,
                   COUNT(DISTINCT CASE WHEN a.type = 'EXAM' THEN g.id END) AS nb_examens,
                   SUM(CASE WHEN a.type='QUIZ'       THEN (g.score/NULLIF(g.max_score,0)*20)*0.20 ELSE 0 END)
                 + SUM(CASE WHEN a.type='ASSIGNMENT' THEN (g.score/NULLIF(g.max_score,0)*20)*0.30 ELSE 0 END)
                 + SUM(CASE WHEN a.type='EXAM'       THEN (g.score/NULLIF(g.max_score,0)*20)*0.50 ELSE 0 END)
                   AS note_finale
            FROM grade g
            JOIN assessment a ON g.assessment_id = a.id
            WHERE g.max_score > 0
            GROUP BY g.student_email
        """, conn)
    finally:
        conn.close()

    # Step 4 — payment metrics
    conn = _my(MY["payment"])
    try:
        payments = pd.read_sql(
            "SELECT student_email, enrollment_id, amount, status, date FROM payment_entity",
            conn,
        )
    finally:
        conn.close()

    conn = _pg(PG["enrollment"])
    try:
        enrollments = pd.read_sql(
            "SELECT id AS enrollment_id, enrolled_at FROM enrollments",
            conn,
        )
    finally:
        conn.close()

    payments["enrollment_id"] = payments["enrollment_id"].astype(str)
    enrollments["enrollment_id"] = enrollments["enrollment_id"].astype(str)
    payments = payments.merge(enrollments, on="enrollment_id", how="left")

    payments["date"] = pd.to_datetime(payments["date"], errors="coerce")
    payments["enrolled_at"] = pd.to_datetime(payments["enrolled_at"], errors="coerce")
    now = pd.Timestamp.now()

    def _delay(row):
        if pd.isna(row["enrolled_at"]):
            return 0
        ref = row["date"] if row["status"] == "PAID" and not pd.isna(row["date"]) else now
        return max(0, (ref - row["enrolled_at"]).days)

    payments["delay_days"] = payments.apply(_delay, axis=1)

    pay_rows = []
    for email, grp in payments.groupby("student_email"):
        paid = grp[grp["status"] == "PAID"]
        unpaid = grp[grp["status"].isin(["PENDING", "FAILED"])]
        n_total = len(grp)
        last_paid = paid["date"].max() if len(paid) > 0 else None
        pay_rows.append({
            "email": email,
            "retard_moyen": float(grp["delay_days"].mean()) if n_total > 0 else 0.0,
            "retard_max":   float(grp["delay_days"].max())  if n_total > 0 else 0.0,
            "nb_impayés":   len(unpaid),
            "taux_paiement": len(paid) / n_total if n_total > 0 else 1.0,
            "montant_impayé": float(unpaid["amount"].sum()),
            "date_dernier_paiement": (
                last_paid.strftime("%d %b %Y")
                if last_paid is not None and not pd.isna(last_paid)
                else ""
            ),
        })
    pay_df = pd.DataFrame(pay_rows) if pay_rows else pd.DataFrame(
        columns=["email", "retard_moyen", "retard_max", "nb_impayés",
                 "taux_paiement", "montant_impayé", "date_dernier_paiement"]
    )

    # Merge all on email
    df = students.merge(
        attendance.rename(columns={"student_email": "email"}), on="email", how="left"
    )
    df = df.merge(
        grades.rename(columns={"student_email": "email"}), on="email", how="left"
    )
    df = df.merge(pay_df, on="email", how="left")

    # Fill missing payment/attendance columns
    for col in ["retard_moyen", "retard_max", "nb_impayés", "montant_impayé"]:
        df[col] = df[col].fillna(0)
    df["taux_paiement"]          = df["taux_paiement"].fillna(1.0)
    df["date_dernier_paiement"]  = df["date_dernier_paiement"].fillna("")
    df["nb_seances"]             = df["nb_seances"].fillna(0).astype(int)
    df["nb_absences"]            = df["nb_absences"].fillna(0).astype(int)
    df["taux_absence"]           = df["taux_absence"].fillna(0.0)
    df["nb_examens"]             = df["nb_examens"].fillna(0).astype(int)

    # Step 5 — derived columns
    df["age"]            = df["email"].apply(derive_age)
    df["genre"]          = df["email"].apply(derive_genre)
    df["genre_encoded"]  = df["genre"].map({"M": 0, "F": 1})
    df["niveau_encoded"] = df["niveau"].map(NIVEAU_MAP).fillna(0).astype(int)
    df["dropout"]        = ((df["taux_absence"] > 0.50) | (df["note_moyenne"] < 7.0)).astype(int)
    df["payment_risk"]   = ((df["nb_impayés"] >= 2) | (df["retard_moyen"] > 30)).astype(int)

    # Step 6 — fill remaining missing values and save
    df[FEATURE_COLS] = df[FEATURE_COLS].fillna(df[FEATURE_COLS].median())
    df["note_finale"] = df["note_finale"].fillna(df["note_moyenne"])

    ordered_cols = [
        "etudiant_id", "email", "nom", "prenom", "age", "genre", "niveau", "class_name",
        "statut", "taux_absence", "nb_seances", "nb_absences",
        "note_moyenne", "note_min", "note_max", "note_std", "nb_examens",
        "retard_moyen", "retard_max", "nb_impayés", "taux_paiement",
        "montant_impayé", "date_dernier_paiement",
        "niveau_encoded", "genre_encoded", "dropout", "payment_risk", "note_finale",
    ]
    for col in ordered_cols:
        if col not in df.columns:
            df[col] = None
    df = df[ordered_cols]

    os.makedirs(os.path.dirname(DATASET_PATH) or ".", exist_ok=True)
    df.to_csv(DATASET_PATH, index=False, encoding="utf-8")
    return df


# ── routes ─────────────────────────────────────────────────────────────────────

@router.post("/dataset/build")
def build():
    try:
        df = build_dataset()
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))

    # Retrain all DSO models with fresh data
    import dso1, dso2, dso3, dso4
    dso1.reload()
    dso2.reload()
    dso3.reload()
    dso4.reload()

    n = len(df)
    return {
        "status": "ok",
        "rows": n,
        "dropout_rate": round(float(df["dropout"].mean()), 3) if n > 0 else 0.0,
        "payment_risk_rate": round(float(df["payment_risk"].mean()), 3) if n > 0 else 0.0,
        "built_at": datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
        "csv_path": DATASET_PATH,
    }


@router.get("/dataset/status")
def status():
    if not os.path.exists(DATASET_PATH):
        return {"exists": False}
    stat = os.stat(DATASET_PATH)
    df = pd.read_csv(DATASET_PATH, encoding="utf-8")
    return {
        "exists": True,
        "rows": len(df),
        "columns": len(df.columns),
        "built_at": datetime.fromtimestamp(stat.st_mtime).strftime("%Y-%m-%dT%H:%M:%S"),
        "size_kb": round(stat.st_size / 1024, 1),
    }
