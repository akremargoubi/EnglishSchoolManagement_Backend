import os
import hashlib
import io

import pandas as pd
from fastapi import APIRouter
from fastapi.responses import JSONResponse, Response
from sklearn.ensemble import RandomForestClassifier

from config import DATASET_PATH, FEATURE_COLS, AVATAR_COLORS, NIVEAU_MAP

router = APIRouter()

CLUSTER_COLORS = ["#1D9E75", "#BA7517", "#D85A30"]
CLUSTER_LABELS = ["Excellent", "Average", "At risk"]


# ── shared helpers ─────────────────────────────────────────────────────────────

def load_df() -> pd.DataFrame:
    return pd.read_csv(DATASET_PATH, encoding="utf-8")

def initials(name: str) -> str:
    parts = name.strip().split()
    return (parts[0][0] + parts[-1][0]).upper() if len(parts) >= 2 else name[:2].upper()

def avatar_color(email: str) -> str:
    h = int(hashlib.md5(email.encode()).hexdigest()[:4], 16)
    return AVATAR_COLORS[h % len(AVATAR_COLORS)]

def risk_level_dropout(prob: float) -> str:
    if prob >= 0.80: return "critical"
    if prob >= 0.60: return "high"
    if prob >= 0.40: return "moderate"
    return "low"

def risk_level_payment(score: float) -> str:
    if score >= 0.70: return "high"
    if score >= 0.45: return "medium"
    return "low"


# ── model lifecycle ────────────────────────────────────────────────────────────

_model = None


def _train():
    global _model
    if not os.path.exists(DATASET_PATH):
        _model = None
        return
    df = load_df()
    X = df[FEATURE_COLS].fillna(df[FEATURE_COLS].median())
    y = df["payment_risk"].astype(int)
    clf = RandomForestClassifier(n_estimators=100, random_state=42)
    clf.fit(X, y)
    _model = clf


def reload():
    _train()


_train()


# ── internal helpers ───────────────────────────────────────────────────────────

def _not_ready():
    return {"error": "Dataset not built yet. Call POST /ml/dataset/build first."}

def _get_predictions():
    df = load_df()
    X = df[FEATURE_COLS].fillna(df[FEATURE_COLS].median())
    scores = _model.predict_proba(X)[:, 1]
    return df, scores

def _total_payments(nb_imp: int, taux: float) -> int:
    if nb_imp == 0:
        return max(1, nb_imp)
    if taux >= 1.0:
        return nb_imp
    denom = 1.0 - taux
    if denom < 0.001:
        denom = 0.001
    return max(nb_imp, round(nb_imp / denom))


# ── routes ─────────────────────────────────────────────────────────────────────

@router.get("/summary")
def summary():
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df, scores = _get_predictions()
    df = df.copy()
    df["risk_score"] = scores
    df["risk_level"] = df["risk_score"].apply(risk_level_payment)

    total      = len(df)
    high_count = int((df["risk_level"] == "high").sum())
    med_count  = int((df["risk_level"] == "medium").sum())
    low_count  = int((df["risk_level"] == "low").sum())

    rm = df["retard_moyen"]
    overdue_mask = rm > 0

    def bucket(lo, hi):
        if hi is None:
            return int((rm > lo).sum())
        return int(((rm > lo) & (rm <= hi)).sum())

    return {
        "high_risk_count":        high_count,
        "medium_risk_count":      med_count,
        "low_risk_count":         low_count,
        "total_students":         total,
        "overdue_total_tnd":      round(float(df["montant_impayé"].sum()), 2),
        "overdue_60_days_count":  int((df["retard_max"] > 60).sum()),
        "collection_rate":        round(float(df["taux_paiement"].mean()), 2),
        "avg_delay_days":         int(round(float(rm[overdue_mask].mean()))) if overdue_mask.any() else 0,
        "total_overdue_students": int((df["nb_impayés"] > 0).sum()),
        "donut": {
            "high":   {"count": high_count, "percentage": round(high_count / total, 2) if total else 0},
            "medium": {"count": med_count,  "percentage": round(med_count  / total, 2) if total else 0},
            "low":    {"count": low_count,  "percentage": round(low_count  / total, 2) if total else 0},
        },
        "aging_buckets": [
            {"range": "0–15",  "count": bucket(0,  15)},
            {"range": "16–30", "count": bucket(15, 30)},
            {"range": "31–60", "count": bucket(30, 60)},
            {"range": ">60",   "count": bucket(60, None)},
        ],
    }


@router.get("/students")
def students(risk_level: str = "all", level: str = "all", search: str = ""):
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df, scores = _get_predictions()
    df = df.copy()
    df["risk_score"] = scores
    df["risk_level"] = df["risk_score"].apply(risk_level_payment)

    if risk_level != "all":
        df = df[df["risk_level"] == risk_level]
    if level != "all":
        df = df[df["niveau"] == level]
    if search:
        s = search.lower()
        mask = (
            df["nom"].str.lower().str.contains(s, na=False) |
            df["prenom"].str.lower().str.contains(s, na=False)
        )
        df = df[mask]

    df = df.sort_values("risk_score", ascending=False)

    result = []
    for _, row in df.iterrows():
        result.append({
            "student_id":          str(row["etudiant_id"]),
            "email":               row["email"],
            "name":                f"{row['prenom']} {row['nom']}",
            "initials":            initials(f"{row['prenom']} {row['nom']}"),
            "avatar_color":        avatar_color(row["email"]),
            "level":               row["niveau"] if pd.notna(row["niveau"]) else "",
            "risk_score":          round(float(row["risk_score"]), 2),
            "risk_level":          row["risk_level"],
            "overdue_tnd":         round(float(row["montant_impayé"]), 2) if pd.notna(row["montant_impayé"]) else 0.0,
            "delay_days":          int(round(float(row["retard_moyen"]))),
            "last_payment_date":   row["date_dernier_paiement"] if pd.notna(row["date_dernier_paiement"]) else "",
            "nb_impayés":          int(row["nb_impayés"]),
            "taux_paiement":       round(float(row["taux_paiement"]), 2),
        })
    return result


@router.get("/students/{student_id}")
def student_detail(student_id: str):
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df, scores = _get_predictions()
    df = df.copy()
    df["risk_score"] = scores

    match = df[df["etudiant_id"].astype(str) == student_id]
    if len(match) == 0:
        return JSONResponse(status_code=404, content={"error": "Student not found"})

    row    = match.iloc[0]
    nb_imp = int(row["nb_impayés"])
    taux   = float(row["taux_paiement"])

    return {
        "student_id":          str(row["etudiant_id"]),
        "email":               row["email"],
        "name":                f"{row['prenom']} {row['nom']}",
        "initials":            initials(f"{row['prenom']} {row['nom']}"),
        "avatar_color":        avatar_color(row["email"]),
        "level":               row["niveau"] if pd.notna(row["niveau"]) else "",
        "risk_score":          round(float(row["risk_score"]), 2),
        "risk_level":          risk_level_payment(float(row["risk_score"])),
        "overdue_tnd":         round(float(row["montant_impayé"]), 2) if pd.notna(row["montant_impayé"]) else 0.0,
        "delay_days":          int(round(float(row["retard_moyen"]))),
        "last_payment_date":   row["date_dernier_paiement"] if pd.notna(row["date_dernier_paiement"]) else "",
        "nb_impayés":          nb_imp,
        "taux_paiement":       round(taux, 2),
        "retard_max":          int(round(float(row["retard_max"]))),
        "montant_impayé":      round(float(row["montant_impayé"]), 2) if pd.notna(row["montant_impayé"]) else 0.0,
        "total_payments":      _total_payments(nb_imp, taux),
        "note_moyenne":        round(float(row["note_moyenne"]), 1) if pd.notna(row["note_moyenne"]) else 0.0,
        "taux_absence":        round(float(row["taux_absence"]), 2),
        "age":                 int(row["age"]),
        "genre":               row["genre"] if pd.notna(row["genre"]) else "",
        "class_name":          row["class_name"] if pd.notna(row["class_name"]) else "",
    }


@router.get("/export")
def export():
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df, scores = _get_predictions()
    df = df.copy()
    df["risk_score"] = scores
    df["risk_level"] = df["risk_score"].apply(risk_level_payment)

    out = df[df["risk_level"] != "low"].sort_values("risk_score", ascending=False).copy()
    out["name"] = out["prenom"].astype(str) + " " + out["nom"].astype(str)

    export_df = out[["name", "email", "niveau", "risk_score", "risk_level",
                     "montant_impayé", "retard_moyen", "date_dernier_paiement"]].copy()
    export_df.columns = ["name", "email", "level", "risk_score", "risk_level",
                         "overdue_tnd", "delay_days", "last_payment_date"]

    buf = io.StringIO()
    export_df.to_csv(buf, index=False)
    csv_content = buf.getvalue()

    return Response(
        content=csv_content,
        media_type="text/csv",
        headers={"Content-Disposition": 'attachment; filename="overdue_students.csv"'},
    )
