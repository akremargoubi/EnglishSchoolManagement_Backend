import os
import hashlib

import numpy as np
import pandas as pd
from fastapi import APIRouter
from fastapi.responses import JSONResponse
from sklearn.cluster import KMeans
from sklearn.decomposition import PCA
from sklearn.metrics import silhouette_score
from sklearn.preprocessing import StandardScaler

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


# ── cluster styling ────────────────────────────────────────────────────────────
# cluster id 0 = Excellent, 1 = Average, 2 = At-risk (assigned by mean note)

_CLUSTER_LONG  = {0: "Excellent students", 1: "Average students", 2: "At-risk students"}
_CLUSTER_SHORT = {0: "Excellent",          1: "Average",          2: "At risk"}
_CLUSTER_COLOR = {0: "#1D9E75",            1: "#BA7517",          2: "#D85A30"}
_CLUSTER_BG    = {0: "#E1F5EE",            1: "#FAEEDA",          2: "#FAECE7"}
_CLUSTER_TEXT  = {0: "#085041",            1: "#412402",          2: "#4A1B0C"}


# ── model lifecycle ────────────────────────────────────────────────────────────

_kmeans            = None
_scaler            = None
_pca_model         = None
_df_clustered      = None
_silhouette_val    = None
_variance_explained = None
_raw_to_semantic: dict = {}


def _train():
    global _kmeans, _scaler, _pca_model, _df_clustered
    global _silhouette_val, _variance_explained, _raw_to_semantic

    if not os.path.exists(DATASET_PATH):
        _kmeans = None
        return

    df = load_df()
    X = df[FEATURE_COLS].fillna(df[FEATURE_COLS].median())

    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)

    km = KMeans(n_clusters=3, n_init=10, init="k-means++", random_state=42)
    km.fit(X_scaled)
    raw_labels = km.labels_

    # Assign semantic cluster ids: rank by mean note_finale (highest → 0)
    df2 = df.copy()
    df2["_raw"] = raw_labels
    means = df2.groupby("_raw")["note_finale"].mean().sort_values(ascending=False)
    raw_to_sem = {int(raw): sem for sem, (raw, _) in enumerate(means.items())}
    df2["cluster"] = df2["_raw"].map(raw_to_sem)

    pca = PCA(n_components=2)
    X_pca = pca.fit_transform(X_scaled)
    df2["pc1"] = X_pca[:, 0]
    df2["pc2"] = X_pca[:, 1]

    sil = float(silhouette_score(X_scaled, raw_labels))
    var = [round(float(v), 3) for v in pca.explained_variance_ratio_]

    _kmeans             = km
    _scaler             = scaler
    _pca_model          = pca
    _df_clustered       = df2
    _silhouette_val     = round(sil, 3)
    _variance_explained = var
    _raw_to_semantic    = raw_to_sem


def reload():
    _train()


_train()


# ── internal helpers ───────────────────────────────────────────────────────────

def _not_ready():
    return {"error": "Dataset not built yet. Call POST /ml/dataset/build first."}


# ── routes ─────────────────────────────────────────────────────────────────────

@router.get("/clusters")
def clusters():
    if _kmeans is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df = _df_clustered
    total = len(df)
    result = []
    for cid in [0, 1, 2]:
        grp = df[df["cluster"] == cid]
        n   = len(grp)
        result.append({
            "cluster_id":        cid,
            "label":             _CLUSTER_LONG[cid],
            "color":             _CLUSTER_COLOR[cid],
            "bg_color":          _CLUSTER_BG[cid],
            "text_color":        _CLUSTER_TEXT[cid],
            "count":             n,
            "percentage":        round(n / total, 2) if total > 0 else 0.0,
            "avg_absence_pct":   round(float(grp["taux_absence"].mean() * 100), 1) if n > 0 else 0.0,
            "avg_note":          round(float(grp["note_moyenne"].mean()), 1)        if n > 0 else 0.0,
            "avg_retard_days":   round(float(grp["retard_moyen"].mean()), 1)        if n > 0 else 0.0,
            "dropout_rate":      round(float(grp["dropout"].mean()), 2)             if n > 0 else 0.0,
            "payment_risk_rate": round(float(grp["payment_risk"].mean()), 2)        if n > 0 else 0.0,
        })
    return result


@router.get("/students")
def students(cluster: str = "all", level: str = "all", search: str = ""):
    if _kmeans is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df = _df_clustered.copy()

    if cluster != "all":
        try:
            cid = int(cluster)
            df = df[df["cluster"] == cid]
        except ValueError:
            pass
    if level != "all":
        df = df[df["niveau"] == level]
    if search:
        s = search.lower()
        mask = (
            df["nom"].str.lower().str.contains(s, na=False) |
            df["prenom"].str.lower().str.contains(s, na=False)
        )
        df = df[mask]

    result = []
    for _, row in df.iterrows():
        cid = int(row["cluster"])
        result.append({
            "student_id":    str(row["etudiant_id"]),
            "email":         row["email"],
            "name":          f"{row['prenom']} {row['nom']}",
            "initials":      initials(f"{row['prenom']} {row['nom']}"),
            "avatar_color":  avatar_color(row["email"]),
            "level":         row["niveau"] if pd.notna(row["niveau"]) else "",
            "cluster":       cid,
            "cluster_label": _CLUSTER_SHORT[cid],
            "cluster_color": _CLUSTER_COLOR[cid],
            "cluster_bg":    _CLUSTER_BG[cid],
            "cluster_text":  _CLUSTER_TEXT[cid],
            "absence_pct":   f"{round(float(row['taux_absence']) * 100)}%",
            "note":          round(float(row["note_moyenne"]) if pd.notna(row["note_moyenne"]) else 0.0, 1),
            "retard_jours":  f"{round(float(row['retard_moyen']))}j",
            "pc1":           round(float(row["pc1"]), 2),
            "pc2":           round(float(row["pc2"]), 2),
        })
    return result


@router.get("/pca")
def pca_view():
    if _kmeans is None:
        return JSONResponse(status_code=503, content=_not_ready())
    df = _df_clustered

    points = [
        {
            "x":       round(float(row["pc1"]), 2),
            "y":       round(float(row["pc2"]), 2),
            "cluster": int(row["cluster"]),
            "name":    f"{row['prenom']} {row['nom']}",
            "email":   row["email"],
        }
        for _, row in df.iterrows()
    ]

    # Transform KMeans cluster centers (in scaled space) to PCA space
    centers_pca = _pca_model.transform(_kmeans.cluster_centers_)
    centroids = [
        {
            "x":       round(float(centers_pca[raw_id, 0]), 2),
            "y":       round(float(centers_pca[raw_id, 1]), 2),
            "cluster": _raw_to_semantic.get(raw_id, raw_id),
        }
        for raw_id in range(3)
    ]

    return {
        "points":             points,
        "centroids":          centroids,
        "variance_explained": _variance_explained,
        "silhouette_score":   _silhouette_val,
    }


@router.get("/summary")
def summary():
    if _kmeans is None:
        return JSONResponse(status_code=503, content=_not_ready())
    return {
        "k":                    3,
        "silhouette_score":     _silhouette_val,
        "total_students":       len(_df_clustered),
        "variance_explained_pc1": _variance_explained[0],
        "variance_explained_pc2": _variance_explained[1],
    }
