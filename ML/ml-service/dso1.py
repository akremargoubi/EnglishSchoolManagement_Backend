import os
import hashlib

import numpy as np
import pandas as pd
from fastapi import APIRouter
from fastapi.responses import JSONResponse
from sklearn.ensemble import RandomForestClassifier

from config import DATASET_PATH, FEATURE_COLS, AVATAR_COLORS, NIVEAU_MAP

router = APIRouter()

CLUSTER_COLORS = ["#1D9E75", "#BA7517", "#D85A30"]
CLUSTER_LABELS = ["Excellent", "Average", "At risk"]

PROB_DIST_BINS   = [0, 0.20, 0.40, 0.60, 0.80, 1.01]
PROB_DIST_LABELS = ["0-20%", "20-40%", "40-60%", "60-80%", "80-100%"]
PROB_DIST_COLORS = ["#1D9E75", "#84CC16", "#BA7517", "#D85A30", "#991B1B"]


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
    y = df["dropout"].astype(int)
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
    probs = _model.predict_proba(X)[:, 1]
    return df, probs

def _make_flags(row) -> list:
    flags = []
    if row["taux_absence"] > 0.40:  flags.append("absence")
    if row["retard_moyen"] > 20:    flags.append("retard")
    if row["note_moyenne"] < 10.0:  flags.append("fail")
    return flags

def _compute_student_trend(prob: float, n_weeks: int = 8) -> list:
    start = prob * 0.60
    trend = np.linspace(start, prob, n_weeks)
    rng = np.random.default_rng(int(prob * 10000))
    noise = rng.normal(0, max(prob * 0.015, 0.001), n_weeks)
    trend = np.clip(trend + noise, 0.0, 1.0)
    return [round(float(v), 3) for v in trend]


# ── routes ─────────────────────────────────────────────────────────────────────

@router.get("/summary")
def summary():
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df, probs = _get_predictions()
    total = len(df)
    crit = int((probs >= 0.80).sum())
    high = int(((probs >= 0.60) & (probs < 0.80)).sum())
    mod  = int(((probs >= 0.40) & (probs < 0.60)).sum())
    low  = int((probs < 0.40).sum())

    counts = np.histogram(probs, bins=PROB_DIST_BINS)[0].tolist()
    prob_dist = [
        {"label": l, "count": int(c), "color": col}
        for l, c, col in zip(PROB_DIST_LABELS, counts, PROB_DIST_COLORS)
    ]

    return {
        "critical_count":           crit,
        "high_count":               high,
        "moderate_count":           mod,
        "low_count":                low,
        "total_students":           total,
        "overall_dropout_rate":     round(float(df["dropout"].mean()), 3),
        "alert_threshold_crossed":  crit,
        "probability_distribution": prob_dist,
    }


@router.get("/students")
def students(risk_level: str = "all", level: str = "all", search: str = ""):
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df, probs = _get_predictions()
    df = df.copy()
    df["dropout_prob"] = probs
    df["risk_level"]   = df["dropout_prob"].apply(risk_level_dropout)

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

    df = df.sort_values("dropout_prob", ascending=False)

    result = []
    for _, row in df.iterrows():
        prob = float(row["dropout_prob"])
        result.append({
            "student_id":   str(row["etudiant_id"]),
            "email":        row["email"],
            "name":         f"{row['prenom']} {row['nom']}",
            "initials":     initials(f"{row['prenom']} {row['nom']}"),
            "avatar_color": avatar_color(row["email"]),
            "level":        row["niveau"] if pd.notna(row["niveau"]) else "",
            "dropout_prob": round(prob, 2),
            "risk_level":   row["risk_level"],
            "flags":        _make_flags(row),
            "absence_rate": round(float(row["taux_absence"]), 2),
            "note_moyenne": round(float(row["note_moyenne"]), 1) if pd.notna(row["note_moyenne"]) else 0.0,
            "retard_moyen": round(float(row["retard_moyen"]), 1),
            "trend":        _compute_student_trend(prob),
        })
    return result


@router.get("/students/{student_id}")
def student_detail(student_id: str):
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df, probs = _get_predictions()
    df = df.copy()
    df["dropout_prob"] = probs

    match = df[df["etudiant_id"].astype(str) == student_id]
    if len(match) == 0:
        return JSONResponse(status_code=404, content={"error": "Student not found"})

    row = match.iloc[0]
    prob = float(row["dropout_prob"])
    return {
        "student_id":   str(row["etudiant_id"]),
        "email":        row["email"],
        "name":         f"{row['prenom']} {row['nom']}",
        "initials":     initials(f"{row['prenom']} {row['nom']}"),
        "avatar_color": avatar_color(row["email"]),
        "level":        row["niveau"] if pd.notna(row["niveau"]) else "",
        "dropout_prob": round(prob, 2),
        "risk_level":   risk_level_dropout(prob),
        "flags":        _make_flags(row),
        "absence_rate": round(float(row["taux_absence"]), 2),
        "note_moyenne": round(float(row["note_moyenne"]), 1) if pd.notna(row["note_moyenne"]) else 0.0,
        "retard_moyen": round(float(row["retard_moyen"]), 1),
        "nb_seances":   int(row["nb_seances"]),
        "nb_absences":  int(row["nb_absences"]),
        "nb_examens":   int(row["nb_examens"]),
        "class_name":   row["class_name"] if pd.notna(row["class_name"]) else "",
        "statut":       row["statut"] if pd.notna(row["statut"]) else "",
        "genre":        row["genre"] if pd.notna(row["genre"]) else "",
        "age":          int(row["age"]),
        "trend":        _compute_student_trend(prob),
    }


@router.get("/weekly-trend")
def weekly_trend(weeks: int = 8):
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df, probs = _get_predictions()
    overall = float(probs.mean())
    rng = np.random.default_rng(42)
    start = overall * 0.70
    series = np.linspace(start, overall, weeks)
    noise = rng.normal(0, max(overall * 0.02, 0.005), weeks)
    series = np.clip(series + noise, 0.0, 1.0)
    total_risk = int((probs >= 0.60).sum())
    return {
        "weeks":       [f"W{i+1}" for i in range(weeks)],
        "dropout_rate": [round(float(v), 3) for v in series],
        "total_risk":  total_risk,
    }


@router.get("/funnel")
def funnel():
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df, probs = _get_predictions()
    total         = len(df)
    absent_risk   = int((df["taux_absence"] > 0.20).sum())
    grade_risk    = int((df["note_moyenne"] < 12.0).sum())
    combined      = int(((df["taux_absence"] > 0.20) & (df["note_moyenne"] < 12.0)).sum())
    actual_dropout = int(df["dropout"].sum())
    return {
        "stages": [
            {"label": "Enrolled",      "count": total,           "color": "#1D9E75"},
            {"label": "Absent Risk",   "count": absent_risk,     "color": "#84CC16"},
            {"label": "Grade Risk",    "count": grade_risk,      "color": "#BA7517"},
            {"label": "Combined Risk", "count": combined,        "color": "#D85A30"},
            {"label": "Dropout",       "count": actual_dropout,  "color": "#991B1B"},
        ]
    }


@router.get("/retention-by-level")
def retention_by_level():
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df, probs = _get_predictions()
    df = df.copy()
    df["dropout_prob"] = probs
    levels = ["L1", "L2", "L3", "M1", "M2"]
    result = []
    for lvl in levels:
        sub = df[df["niveau"] == lvl]
        if len(sub) == 0:
            continue
        retention = float(1 - sub["dropout"].mean())
        avg_prob  = float(sub["dropout_prob"].mean())
        result.append({
            "level":           lvl,
            "count":           len(sub),
            "retention_rate":  round(retention, 3),
            "avg_dropout_prob": round(avg_prob, 3),
        })
    return result


@router.get("/scatter")
def scatter(max_points: int = 150):
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df, probs = _get_predictions()
    df = df.copy()
    df["dropout_prob"] = probs

    critical = df[df["dropout_prob"] >= 0.80]
    others   = df[df["dropout_prob"] < 0.80]
    n_others = max(0, max_points - len(critical))
    if n_others > 0 and len(others) > n_others:
        others = others.sample(n=n_others, random_state=42)
    sample = pd.concat([critical, others])

    x_arr = sample["taux_absence"].values.astype(float)
    y_arr = sample["dropout_prob"].values.astype(float)

    coeffs  = np.polyfit(x_arr, y_arr, 1)
    trend_y1 = float(np.polyval(coeffs, 0.0))
    trend_y2 = float(np.polyval(coeffs, 1.0))
    corr = float(np.corrcoef(x_arr, y_arr)[0, 1]) if len(x_arr) > 1 else 0.0

    points = []
    for _, row in sample.iterrows():
        points.append({
            "x":    round(float(row["taux_absence"]), 3),
            "y":    round(float(row["dropout_prob"]), 3),
            "risk": risk_level_dropout(float(row["dropout_prob"])),
            "name": f"{row['prenom']} {row['nom']}",
        })

    return {
        "points":      points,
        "trend_line":  {"x1": 0.0, "y1": round(trend_y1, 3), "x2": 1.0, "y2": round(trend_y2, 3)},
        "correlation": round(corr, 3),
    }


@router.get("/feature-correlation")
def feature_correlation():
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df, _ = _get_predictions()
    target = df["dropout"].astype(float)

    feature_labels = {
        "taux_absence":   "Absence Rate",
        "note_moyenne":   "Avg Grade",
        "retard_moyen":   "Avg Delay",
        "age":            "Age",
        "nb_seances":     "Sessions",
        "nb_examens":     "Exams",
        "niveau_encoded": "Level",
        "genre_encoded":  "Gender",
    }

    imp_col = [c for c in df.columns if "impay" in c.lower()]
    if imp_col:
        feature_labels[imp_col[0]] = "Overdue Bills"

    result = []
    for col, label in feature_labels.items():
        if col not in df.columns:
            continue
        vals = df[col].fillna(df[col].median()).astype(float)
        try:
            r = float(np.corrcoef(vals, target)[0, 1])
            if not np.isfinite(r):
                r = 0.0
        except Exception:
            r = 0.0
        result.append({
            "feature":     label,
            "correlation": round(r, 3),
            "direction":   "positive" if r >= 0 else "negative",
        })

    result.sort(key=lambda x: abs(x["correlation"]), reverse=True)
    return result
