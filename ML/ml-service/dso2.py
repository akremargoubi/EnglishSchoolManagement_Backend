import os
import hashlib

import numpy as np
import pandas as pd
from fastapi import APIRouter
from fastapi.responses import JSONResponse
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.metrics import mean_squared_error

from config import DATASET_PATH, FEATURE_COLS, AVATAR_COLORS, NIVEAU_MAP

router = APIRouter()

CLUSTER_COLORS = ["#1D9E75", "#BA7517", "#D85A30"]
CLUSTER_LABELS = ["Excellent", "Average", "At risk"]

FEATURE_NAMES = {
    "note_moyenne":   "Avg. session note",
    "taux_absence":   "Absence rate",
    "nb_seances":     "Sessions attended",
    "nb_examens":     "Exams taken",
    "retard_moyen":   "Payment delay",
    "age":            "Age",
    "niveau_encoded": "Level",
    "genre_encoded":  "Gender",
}


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
_rmse  = None
_feature_importances: dict = {}


def _train():
    global _model, _rmse, _feature_importances
    if not os.path.exists(DATASET_PATH):
        _model = None
        return
    df = load_df()
    df_clean = df.dropna(subset=["note_finale"])
    X = df_clean[FEATURE_COLS].fillna(df_clean[FEATURE_COLS].median())
    y = df_clean["note_finale"]
    reg = GradientBoostingRegressor(
        n_estimators=100, learning_rate=0.1, max_depth=3, random_state=42
    )
    reg.fit(X, y)
    preds = reg.predict(X)
    _rmse = float(np.sqrt(mean_squared_error(y, preds)))
    _feature_importances = dict(zip(FEATURE_COLS, reg.feature_importances_.tolist()))
    _model = reg


def reload():
    _train()


_train()


# ── internal helpers ───────────────────────────────────────────────────────────

def _not_ready():
    return {"error": "Dataset not built yet. Call POST /ml/dataset/build first."}

def _grade_status(predicted: float, current_avg: float) -> str:
    if predicted > current_avg + 0.5: return "improving"
    if predicted < current_avg - 0.5: return "declining"
    return "stable"

def _status_label(predicted: float) -> str:
    if predicted >= 16: return "Excellent"
    if predicted >= 14: return "Very good"
    if predicted >= 12: return "Good"
    if predicted >= 10: return "Average"
    if predicted >= 7:  return "Below average"
    return "Failing"

def _simulate_trend(current_avg: float, predicted: float) -> list:
    delta = predicted - current_avg
    seed = int(abs(current_avg * 100)) % (2**31)
    rng = np.random.default_rng(seed)
    if delta < -0.5:
        base = np.linspace(current_avg, predicted, 5)
        noise = rng.uniform(-0.2, 0.2, 5)
    elif delta > 0.5:
        start = current_avg - abs(delta) * 0.4
        base = np.linspace(start, predicted, 5)
        noise = rng.uniform(-0.2, 0.2, 5)
    else:
        base = np.full(5, current_avg)
        noise = rng.uniform(-0.3, 0.3, 5)
    return [round(float(v), 1) for v in (base + noise)]

def _top_importances(n: int = 5) -> list:
    items = sorted(
        [{"name": FEATURE_NAMES[k], "importance": round(float(v), 2)}
         for k, v in _feature_importances.items()],
        key=lambda x: x["importance"], reverse=True,
    )
    return items[:n]

def _predict_all(df: pd.DataFrame):
    X = df[FEATURE_COLS].fillna(df[FEATURE_COLS].median())
    return _model.predict(X)


# ── routes ─────────────────────────────────────────────────────────────────────

@router.get("/students")
def students(search: str = "", level: str = "all", status: str = "all"):
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df = load_df()
    df = df.copy()
    df["predicted_grade"] = _predict_all(df)

    if level != "all":
        df = df[df["niveau"] == level]
    if search:
        s = search.lower()
        mask = (
            df["nom"].str.lower().str.contains(s, na=False) |
            df["prenom"].str.lower().str.contains(s, na=False)
        )
        df = df[mask]
    if status != "all":
        df = df[df.apply(
            lambda r: _grade_status(r["predicted_grade"],
                                    r["note_moyenne"] if pd.notna(r["note_moyenne"]) else r["predicted_grade"]) == status,
            axis=1,
        )]

    df = df.sort_values("predicted_grade", ascending=True)

    result = []
    for _, row in df.iterrows():
        curr = float(row["note_moyenne"]) if pd.notna(row["note_moyenne"]) else 0.0
        pred = round(float(row["predicted_grade"]), 1)
        result.append({
            "student_id":      str(row["etudiant_id"]),
            "email":           row["email"],
            "name":            f"{row['prenom']} {row['nom']}",
            "initials":        initials(f"{row['prenom']} {row['nom']}"),
            "avatar_color":    avatar_color(row["email"]),
            "level":           row["niveau"] if pd.notna(row["niveau"]) else "",
            "current_avg":     round(curr, 1),
            "predicted_grade": pred,
            "rmse":            round(_rmse, 2),
            "ci_low":          round(pred - _rmse, 2),
            "ci_high":         round(pred + _rmse, 2),
            "trend":           _simulate_trend(curr, pred),
            "status":          _grade_status(pred, curr),
        })
    return result


@router.get("/students/{student_id}")
def student_detail(student_id: str):
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df = load_df()
    df = df.copy()
    df["predicted_grade"] = _predict_all(df)

    match = df[df["etudiant_id"].astype(str) == student_id]
    if len(match) == 0:
        return JSONResponse(status_code=404, content={"error": "Student not found"})

    row = match.iloc[0]
    curr = float(row["note_moyenne"]) if pd.notna(row["note_moyenne"]) else 0.0
    pred = round(float(row["predicted_grade"]), 1)

    return {
        "student_id":          str(row["etudiant_id"]),
        "email":               row["email"],
        "name":                f"{row['prenom']} {row['nom']}",
        "initials":            initials(f"{row['prenom']} {row['nom']}"),
        "avatar_color":        avatar_color(row["email"]),
        "level":               row["niveau"] if pd.notna(row["niveau"]) else "",
        "current_avg":         round(curr, 1),
        "predicted_grade":     pred,
        "rmse":                round(_rmse, 2),
        "ci_low":              round(pred - _rmse, 2),
        "ci_high":             round(pred + _rmse, 2),
        "trend":               _simulate_trend(curr, pred),
        "status":              _grade_status(pred, curr),
        "status_label":        _status_label(pred),
        "feature_importances": _top_importances(),
        "age":                 int(row["age"]),
        "genre":               row["genre"] if pd.notna(row["genre"]) else "",
        "nb_seances":          int(row["nb_seances"]),
        "nb_absences":         int(row["nb_absences"]),
        "retard_moyen":        round(float(row["retard_moyen"]), 1),
        "montant_impayé":      round(float(row["montant_impayé"]), 2) if pd.notna(row["montant_impayé"]) else 0.0,
    }


@router.get("/distribution")
def distribution(level: str = "all"):
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df = load_df()
    df = df.copy()
    df["predicted_grade"] = _predict_all(df)

    if level != "all":
        df = df[df["niveau"] == level]

    pg = df["predicted_grade"]
    buckets = [
        {"range": "0–5",   "count": int((pg < 5).sum())},
        {"range": "5–9",   "count": int(((pg >= 5)  & (pg < 9)).sum())},
        {"range": "10–12", "count": int(((pg >= 9)  & (pg < 13)).sum())},
        {"range": "13–15", "count": int(((pg >= 13) & (pg < 16)).sum())},
        {"range": "16–20", "count": int((pg >= 16).sum())},
    ]
    n = len(df)
    return {
        "level":          level,
        "buckets":        buckets,
        "cohort_avg":     round(float(pg.mean()), 1)   if n > 0 else 0.0,
        "cohort_median":  round(float(pg.median()), 1) if n > 0 else 0.0,
        "fail_rate":      round(float((pg < 10).mean()), 3) if n > 0 else 0.0,
    }


@router.get("/top-declining")
def top_declining():
    if _model is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df = load_df()
    df = df.copy()
    df["predicted_grade"] = _predict_all(df)
    df["_curr"] = df["note_moyenne"].fillna(df["predicted_grade"])
    df["delta"]  = df["predicted_grade"] - df["_curr"]
    df = df.sort_values("delta", ascending=True).head(4)

    result = []
    for _, row in df.iterrows():
        pred  = round(float(row["predicted_grade"]), 1)
        curr  = round(float(row["_curr"]), 1)
        delta = round(float(row["delta"]), 1)
        result.append({
            "name":            f"{row['prenom']} {row['nom']}",
            "predicted_grade": pred,
            "current_avg":     curr,
            "delta":           delta,
            "direction":       "down" if delta < 0 else "up",
            "bar_width_pct":   round(pred / 20 * 100),
        })
    return result
