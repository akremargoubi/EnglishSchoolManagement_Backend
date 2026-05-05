import os

# Individual host/port env vars for each DB (set in docker-compose for container names,
# leave unset for localhost with mapped ports in local dev)
_AUTH_HOST       = os.getenv("ML_AUTH_HOST",       "localhost")
_AUTH_PORT       = int(os.getenv("ML_AUTH_PORT",       "5433"))
_ENROLLMENT_HOST = os.getenv("ML_ENROLLMENT_HOST", "localhost")
_ENROLLMENT_PORT = int(os.getenv("ML_ENROLLMENT_PORT", "5434"))
_ATTENDANCE_HOST = os.getenv("ML_ATTENDANCE_HOST", "localhost")
_ATTENDANCE_PORT = int(os.getenv("ML_ATTENDANCE_PORT", "5436"))
_ASSESSMENT_HOST = os.getenv("ML_ASSESSMENT_HOST", "localhost")
_ASSESSMENT_PORT = int(os.getenv("ML_ASSESSMENT_PORT", "3308"))
_PAYMENT_HOST    = os.getenv("ML_PAYMENT_HOST",    "localhost")
_PAYMENT_PORT    = int(os.getenv("ML_PAYMENT_PORT",    "3309"))

PG = {
    "auth": {
        "host": _AUTH_HOST, "port": _AUTH_PORT, "dbname": "esmauthdb",
        "user": "authserviceuser", "password": "authservicedbp4ss",
    },
    "enrollment": {
        "host": _ENROLLMENT_HOST, "port": _ENROLLMENT_PORT, "dbname": "enrollmentsdb",
        "user": "esm", "password": "esmsecret",
    },
    "attendance": {
        "host": _ATTENDANCE_HOST, "port": _ATTENDANCE_PORT, "dbname": "attendance_db",
        "user": "esm", "password": "esmsecret",
    },
}

MY = {
    "assessment": {
        "host": _ASSESSMENT_HOST, "port": _ASSESSMENT_PORT, "db": "assessment_db",
        "user": "root", "password": "root", "charset": "utf8mb4",
    },
    "payment": {
        "host": _PAYMENT_HOST, "port": _PAYMENT_PORT, "db": "payment_db",
        "user": "root", "password": "root", "charset": "utf8mb4",
    },
}

DATASET_PATH = "data/dataset_ml.csv"
FEATURE_COLS = ["age", "taux_absence", "note_moyenne", "retard_moyen",
                "nb_seances", "nb_examens", "niveau_encoded", "genre_encoded"]
NIVEAU_MAP   = {"L1": 0, "L2": 1, "L3": 2, "M1": 3, "M2": 4}
AVATAR_COLORS = [
    "#7F77DD", "#D85A30", "#1D9E75", "#BA7517", "#378ADD",
    "#993556", "#185FA5", "#854F0B", "#0F6E56", "#A32D2D",
]
