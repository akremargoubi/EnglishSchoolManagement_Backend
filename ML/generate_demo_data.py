#!/usr/bin/env python3
"""
generate_demo_data.py
=====================
Fills all microservice databases with realistic English school demo data.
Designed for ML pipeline demonstration — no schema changes, no microservice edits.

Data volumes (500 students):
  • 504 students across 6 classes — 84 per class
  • 12 courses · 36 assessments · ~3780 grades
  • ~31 500 attendance records · ~1400 payments · ~1260 enrollments
  • Runtime: ~3–6 min depending on machine

Cluster distribution (drives all ML predictions):
  • Cluster 0 — Excellent  (35%, ~176 students): low absence, high grades, on-time payment
  • Cluster 1 — Average    (40%, ~202 students): moderate absence, average grades, some delay
  • Cluster 2 — At-risk    (25%, ~126 students): high absence, low grades, payment issues

Usage:
  pip install faker psycopg2-binary pymysql
  python generate_demo_data.py             # fill databases (safe to re-run)
  python generate_demo_data.py --reset     # wipe all generated data first, then fill
"""

import sys
import random
import hashlib
import uuid
from datetime import datetime, timedelta, date, time
from faker import Faker
import psycopg2
import psycopg2.extras
import pymysql

# ─────────────────────────────────────────────────────────────────────────────
# SEED — same seed = same data every run (reproducible ML results)
# ─────────────────────────────────────────────────────────────────────────────
SEED = 42
random.seed(SEED)
Faker.seed(SEED)
fake = Faker(["en_US"])
NOW   = datetime.utcnow()
TODAY = NOW.date()
RESET = "--reset" in sys.argv

# ── Volume control ───────────────────────────────────────────────────────────
STUDENTS_PER_CLASS  = 84   # 84 × 6 classes = 504 students ≈ 500
SESSIONS_PER_ENROLL = 25   # attendance sessions per enrollment

# ── Progress helper ──────────────────────────────────────────────────────────
import time as _time
_step_t = [0.0]

def step_start(label):
    _step_t[0] = _time.time()
    print(f"  → {label}", end="", flush=True)

def step_done(detail=""):
    elapsed = _time.time() - _step_t[0]
    suffix  = f"  ({detail})" if detail else ""
    print(f"{suffix}  [{elapsed:.1f}s]")

# ─────────────────────────────────────────────────────────────────────────────
# DB CONNECTION CONFIG  (matches docker-compose.yml exactly)
# ─────────────────────────────────────────────────────────────────────────────
PG_CFG = {
    "auth": {
        "host": "localhost", "port": 5433, "dbname": "esmauthdb",
        "user": "authserviceuser", "password": "authservicedbp4ss",
    },
    "enrollment": {
        "host": "localhost", "port": 5434, "dbname": "enrollmentsdb",
        "user": "esm", "password": "esmsecret",
    },
    "course": {
        "host": "localhost", "port": 5435, "dbname": "coursesdb",
        "user": "esm", "password": "esmsecret",
    },
    "attendance": {
        "host": "localhost", "port": 5436, "dbname": "attendance_db",
        "user": "esm", "password": "esmsecret",
    },
    "schedule": {
        "host": "localhost", "port": 5437, "dbname": "schedule_db",
        "user": "esm", "password": "esmsecret",
    },
    "email": {
        "host": "localhost", "port": 5438, "dbname": "email_db",
        "user": "esm", "password": "esmsecret",
    },
}
MY_CFG = {
    "assessment": {
        "host": "localhost", "port": 3308, "db": "assessment_db",
        "user": "root", "password": "root", "charset": "utf8mb4",
    },
    "payment": {
        "host": "localhost", "port": 3309, "db": "payment_db",
        "user": "root", "password": "root", "charset": "utf8mb4",
    },
    "reporting": {
        "host": "localhost", "port": 3310, "db": "reporting_db",
        "user": "root", "password": "root", "charset": "utf8mb4",
    },
    "resources": {
        "host": "localhost", "port": 3307, "db": "resources_db",
        "user": "root", "password": "root", "charset": "utf8mb4",
    },
}

def pg(key):
    return psycopg2.connect(**PG_CFG[key])

def my(key):
    return pymysql.connect(**MY_CFG[key])

def log(msg):
    print(f"     {msg}")

def section(title):
    print(f"\n{'─'*58}")
    print(f"  {title}")
    print(f"{'─'*58}")

# ─────────────────────────────────────────────────────────────────────────────
# CLUSTER PROFILE HELPERS
# ─────────────────────────────────────────────────────────────────────────────
def assign_cluster(idx, total):
    """Deterministically assign cluster based on student index."""
    r = idx / total
    if r < 0.35:
        return 0  # Excellent
    elif r < 0.75:
        return 1  # Average
    else:
        return 2  # At-risk

def absence_rate(cluster):
    if cluster == 0:
        return random.uniform(0.03, 0.18)
    elif cluster == 1:
        return random.uniform(0.20, 0.42)
    else:
        return random.uniform(0.45, 0.78)

def score_factor(cluster):
    """Returns score as fraction of max_score (0..1)."""
    if cluster == 0:
        return random.uniform(0.72, 0.99)
    elif cluster == 1:
        return random.uniform(0.45, 0.72)
    else:
        return random.uniform(0.08, 0.44)

def payment_delay_days(cluster):
    """Days between enrollment and payment."""
    if cluster == 0:
        return random.randint(1, 7)
    elif cluster == 1:
        return random.randint(10, 40)
    else:
        return random.randint(35, 90)

def payment_status(cluster):
    """Payment status distribution per cluster."""
    if cluster == 0:
        return random.choices(["PAID", "PENDING"], weights=[95, 5])[0]
    elif cluster == 1:
        return random.choices(["PAID", "PENDING", "FAILED"], weights=[70, 22, 8])[0]
    else:
        return random.choices(["PAID", "PENDING", "FAILED"], weights=[30, 45, 25])[0]

# ─────────────────────────────────────────────────────────────────────────────
# STEP 0 — RESET (optional)
# ─────────────────────────────────────────────────────────────────────────────
def reset_all():
    section("STEP 0 — Resetting all tables")

    # PostgreSQL: auth-db
    conn = pg("auth")
    cur = conn.cursor()
    cur.execute("DELETE FROM users WHERE role NOT IN ('ADMIN')")
    cur.execute("DELETE FROM classes")
    conn.commit(); cur.close(); conn.close()
    log("auth-db: users, classes cleared")

    # PostgreSQL: attendance-db
    conn = pg("attendance")
    cur = conn.cursor()
    cur.execute("DELETE FROM attendances")
    conn.commit(); cur.close(); conn.close()
    log("attendance-db: attendances cleared")

    # PostgreSQL: enrollment-db
    conn = pg("enrollment")
    cur = conn.cursor()
    cur.execute("DELETE FROM student_progress")
    cur.execute("DELETE FROM progress")
    cur.execute("DELETE FROM certificates")
    cur.execute("DELETE FROM enrollments")
    conn.commit(); cur.close(); conn.close()
    log("enrollment-db: enrollments, progress cleared")

    # PostgreSQL: course-db
    conn = pg("course")
    cur = conn.cursor()
    cur.execute("DELETE FROM reviews")
    cur.execute("DELETE FROM lessons")
    cur.execute("DELETE FROM modules")
    cur.execute("DELETE FROM courses")
    cur.execute("DELETE FROM instructors")
    cur.execute("DELETE FROM categories")
    conn.commit(); cur.close(); conn.close()
    log("course-db: courses, modules, lessons, instructors cleared")

    # PostgreSQL: schedule-db
    conn = pg("schedule")
    cur = conn.cursor()
    cur.execute("DELETE FROM schedules")
    conn.commit(); cur.close(); conn.close()
    log("schedule-db: schedules cleared")

    # MySQL: assessment-db
    conn = my("assessment")
    cur = conn.cursor()
    cur.execute("SET FOREIGN_KEY_CHECKS=0")
    cur.execute("TRUNCATE TABLE grade")
    cur.execute("TRUNCATE TABLE assessment")
    cur.execute("SET FOREIGN_KEY_CHECKS=1")
    conn.commit(); cur.close(); conn.close()
    log("assessment-db: assessment, grade cleared")

    # MySQL: payment-db
    conn = my("payment")
    cur = conn.cursor()
    cur.execute("TRUNCATE TABLE payment_entity")
    conn.commit(); cur.close(); conn.close()
    log("payment-db: payment_entity cleared")

    # MySQL: reporting-db
    conn = my("reporting")
    cur = conn.cursor()
    cur.execute("TRUNCATE TABLE reclamation")
    conn.commit(); cur.close(); conn.close()
    log("reporting-db: reclamation cleared")

    # MySQL: resources-db
    conn = my("resources")
    cur = conn.cursor()
    cur.execute("TRUNCATE TABLE learning_resource")
    conn.commit(); cur.close(); conn.close()
    log("resources-db: learning_resource cleared")

# ─────────────────────────────────────────────────────────────────────────────
# STEP 1 — AUTH-DB: classes + users (tutors + students)
# ─────────────────────────────────────────────────────────────────────────────
def seed_auth():
    section("STEP 1 — auth-db: classes + users")
    conn = pg("auth")
    cur = conn.cursor()

    CLASS_DEFS = [
        ("L1-A", "L1", "General English",      "Level 1 - Group A"),
        ("L1-B", "L1", "General English",      "Level 1 - Group B"),
        ("L2-A", "L2", "Business English",     "Level 2 - Group A"),
        ("L3-A", "L3", "Academic English",     "Level 3 - Group A"),
        ("M1-A", "M1", "Advanced English",     "Master 1 - Group A"),
        ("M2-A", "M2", "Professional English", "Master 2 - Group A"),
    ]

    # ── Tutors: one per class ─────────────────────────────────────────────
    tutor_ids = []
    for i, (cname, level, spec, desc) in enumerate(CLASS_DEFS):
        tutor_uid = str(uuid.uuid4())
        tutor_uuid_str = str(uuid.uuid4())
        email = f"tutor.{cname.lower().replace('-','_')}@esm.edu"
        cur.execute("""
            INSERT INTO users (
                id, uuid, email, password, role,
                first_name, last_name, status, wallet_balance,
                is_email_verified, two_factor_enabled,
                created_at, updated_at
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
            ON CONFLICT (email) DO NOTHING
        """, (
            tutor_uid, tutor_uuid_str, email,
            "$2a$10$hashedpassword", "TUTOR",
            fake.first_name(), fake.last_name(),
            "active", 0.0, True, False, NOW, NOW
        ))
        tutor_ids.append((tutor_uid, cname))

    conn.commit()
    log(f"Inserted {len(tutor_ids)} tutors")

    # ── Classes ──────────────────────────────────────────────────────────
    class_ids = {}
    for i, ((tutor_uid, cname), (cn, level, spec, desc)) in enumerate(
        zip(tutor_ids, CLASS_DEFS)
    ):
        cur.execute("""
            INSERT INTO classes (name, level, specialty, description, created_at, tutor_id)
            VALUES (%s,%s,%s,%s,%s,%s)
            ON CONFLICT DO NOTHING
            RETURNING id
        """, (cn, level, spec, desc, NOW, tutor_uid))
        row = cur.fetchone()
        if row:
            class_ids[cn] = row[0]

    conn.commit()
    log(f"Inserted {len(class_ids)} classes: {list(class_ids.keys())}")

    # If classes already existed, fetch their IDs
    if len(class_ids) < len(CLASS_DEFS):
        cur.execute("SELECT id, name FROM classes")
        for row in cur.fetchall():
            class_ids[row[1]] = row[0]

    # ── Students: STUDENTS_PER_CLASS per class, cluster-distributed ─────────
    students = []
    idx = 0
    total_students = len(CLASS_DEFS) * STUDENTS_PER_CLASS

    step_start(f"Inserting {total_students} students")
    for cname, level, spec, desc in CLASS_DEFS:
        class_id = class_ids.get(cname)
        for j in range(STUDENTS_PER_CLASS):
            cluster = assign_cluster(idx, total_students)
            student_uid = str(uuid.uuid4())
            student_uuid_str = str(uuid.uuid4())
            fn = fake.first_name()
            ln = fake.last_name()
            email = f"student.{fn.lower()}.{ln.lower()}.{idx:03d}@esm.edu"
            phone = f"+216{random.randint(20000000, 99999999)}"

            cur.execute("""
                INSERT INTO users (
                    id, uuid, email, password, role,
                    first_name, last_name, phone_number, status,
                    wallet_balance, is_email_verified, two_factor_enabled,
                    class_id, created_at, updated_at
                ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
                ON CONFLICT (email) DO NOTHING
            """, (
                student_uid, student_uuid_str, email,
                "$2a$10$hashedpassword", "STUDENT",
                fn, ln, phone,
                "active" if cluster < 2 else random.choice(["active", "inactive"]),
                0.0, True, False,
                class_id, NOW - timedelta(days=random.randint(30, 365)),
                NOW
            ))

            students.append({
                "uid":     student_uid,
                "email":   email,
                "name":    f"{fn} {ln}",
                "cluster": cluster,
                "class":   cname,
                "level":   level,
            })
            idx += 1

    conn.commit()
    cur.close()
    conn.close()
    step_done(f"{len(students)} students")
    cluster_counts = [sum(1 for s in students if s["cluster"] == c) for c in range(3)]
    log(f"Clusters → Excellent:{cluster_counts[0]}  Average:{cluster_counts[1]}  At-risk:{cluster_counts[2]}")
    return students, class_ids

# ─────────────────────────────────────────────────────────────────────────────
# STEP 2 — COURSE-DB: categories, instructors, courses, modules, lessons
# ─────────────────────────────────────────────────────────────────────────────
def seed_courses():
    section("STEP 2 — course-db: categories, instructors, courses, modules, lessons")
    conn = pg("course")
    cur = conn.cursor()

    # Categories
    CATS = [
        ("grammar",      "Grammar",         "English grammar skills"),
        ("speaking",     "Speaking",        "Oral communication"),
        ("writing",      "Writing",         "Academic & professional writing"),
        ("reading",      "Reading",         "Comprehension strategies"),
        ("business",     "Business English","Workplace communication"),
        ("exam-prep",    "Exam Preparation","IELTS / TOEFL preparation"),
    ]
    cat_ids = {}
    for slug, name, desc in CATS:
        cur.execute("""
            INSERT INTO categories (name, description, slug, created_at)
            VALUES (%s,%s,%s,%s) ON CONFLICT DO NOTHING RETURNING id
        """, (name, desc, slug, NOW))
        row = cur.fetchone()
        if row:
            cat_ids[slug] = row[0]

    if len(cat_ids) < len(CATS):
        cur.execute("SELECT id, slug FROM categories")
        for r in cur.fetchall():
            cat_ids[r[1]] = r[0]
    conn.commit()
    log(f"Inserted {len(cat_ids)} categories")

    # Instructors
    INST_DATA = [
        ("James",   "Miller",  "james.miller@esm.edu"),
        ("Sarah",   "Johnson", "sarah.johnson@esm.edu"),
        ("David",   "Brown",   "david.brown@esm.edu"),
        ("Emma",    "Wilson",  "emma.wilson@esm.edu"),
        ("Michael", "Davis",   "michael.davis@esm.edu"),
        ("Laura",   "Taylor",  "laura.taylor@esm.edu"),
    ]
    inst_ids = []
    for fn, ln, email in INST_DATA:
        cur.execute("""
            INSERT INTO instructors (first_name, last_name, email, bio, created_at)
            VALUES (%s,%s,%s,%s,%s) ON CONFLICT DO NOTHING RETURNING id
        """, (fn, ln, email, f"Expert English instructor with 10+ years experience.", NOW))
        row = cur.fetchone()
        if row:
            inst_ids.append(row[0])

    if len(inst_ids) < len(INST_DATA):
        cur.execute("SELECT id FROM instructors ORDER BY id")
        inst_ids = [r[0] for r in cur.fetchall()]
    conn.commit()
    log(f"Inserted {len(inst_ids)} instructors")

    # Courses: 2 per level
    COURSE_DEFS = [
        # (title, level, cat_slug, price, description)
        ("English Foundations I",        "L1", "grammar",  350.0, "Core grammar and vocabulary for beginners"),
        ("Speaking Basics",              "L1", "speaking", 300.0, "Build confidence in spoken English"),
        ("Business Communication",       "L2", "business", 450.0, "Professional email and presentation skills"),
        ("Academic Writing L2",          "L2", "writing",  400.0, "Essay structure and argumentation"),
        ("Advanced Grammar & Style",     "L3", "grammar",  500.0, "Complex structures and stylistic choices"),
        ("Reading & Critical Thinking",  "L3", "reading",  480.0, "Academic texts and analysis"),
        ("IELTS Preparation",            "M1", "exam-prep",600.0, "Full IELTS preparation with mock tests"),
        ("Business English Advanced",    "M1", "business", 550.0, "Negotiation, presentations, reports"),
        ("Academic Writing for Masters", "M2", "writing",  620.0, "Thesis and research paper writing"),
        ("Professional Speaking",        "M2", "speaking", 580.0, "Public speaking and debating"),
        ("English Foundations II",       "L1", "reading",  320.0, "Reading comprehension for beginners"),
        ("TOEFL Preparation",            "M1", "exam-prep",600.0, "Intensive TOEFL preparation"),
    ]

    course_ids = {}  # level → [course_id, ...]
    for i, (title, level, cat_slug, price, desc) in enumerate(COURSE_DEFS):
        inst_id = inst_ids[i % len(inst_ids)]
        cat_id  = cat_ids.get(cat_slug, list(cat_ids.values())[0])
        cur.execute("""
            INSERT INTO courses (
                name, level, description, price, is_published,
                rating_avg, rating_count, tutor_email,
                category_id, instructor_id
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING course_id
        """, (
            title, level, desc, price, True,
            round(random.uniform(3.5, 5.0), 2),
            random.randint(10, 150),
            f"inst{i}@esm.edu",
            cat_id, inst_id
        ))
        cid = cur.fetchone()[0]
        course_ids.setdefault(level, []).append(cid)

        # 3 modules per course
        for m_idx in range(3):
            cur.execute("""
                INSERT INTO modules (title, order_index, course_id)
                VALUES (%s,%s,%s) RETURNING id
            """, (f"Module {m_idx+1}: {['Introduction','Core Skills','Advanced Practice'][m_idx]}", m_idx, cid))
            mid = cur.fetchone()[0]

            # 3 lessons per module
            for l_idx, (ltype, ltitle) in enumerate([
                ("VIDEO", "Lecture"),
                ("PDF",   "Study Notes"),
                ("QUIZ",  "Practice Quiz"),
            ]):
                cur.execute("""
                    INSERT INTO lessons (title, content_type, duration_minutes, order_index, module_id)
                    VALUES (%s,%s,%s,%s,%s)
                """, (f"{ltitle} {m_idx+1}.{l_idx+1}", ltype, random.randint(15, 60), l_idx, mid))

    conn.commit()
    cur.close()
    conn.close()
    log(f"Inserted {len(COURSE_DEFS)} courses with modules and lessons")
    return course_ids

# ─────────────────────────────────────────────────────────────────────────────
# STEP 3 — ENROLLMENT-DB
# ─────────────────────────────────────────────────────────────────────────────
def seed_enrollments(students, course_ids):
    section("STEP 3 — enrollment-db: enrollments + student_progress")
    conn = pg("enrollment")
    cur = conn.cursor()

    enrollments = []   # (enrollment_id, student_email, course_id, enrolled_at)
    enroll_counter = 1

    for student in students:
        level  = student["level"]
        avail  = course_ids.get(level, [])
        if not avail:
            continue

        # Each student enrolls in 2-3 courses from their level
        n_courses = random.randint(2, min(3, len(avail)))
        chosen    = random.sample(avail, n_courses)
        enrolled_at = NOW - timedelta(days=random.randint(60, 180))

        for cid in chosen:
            cluster   = student["cluster"]
            status    = "active" if cluster < 2 else random.choice(["active", "active", "dropped"])
            completed = NOW - timedelta(days=random.randint(1, 30)) if status == "completed" else None

            cur.execute("""
                INSERT INTO enrollments (
                    user_id, student_name, course_id, status, enrolled_at, completed_at
                ) VALUES (%s,%s,%s,%s,%s,%s) RETURNING id
            """, (
                enroll_counter,
                student["name"],
                cid,
                status,
                enrolled_at,
                completed
            ))
            eid = cur.fetchone()[0]

            # student_progress
            total_lessons   = 27  # 3 modules × 3 lessons × 3 courses per module
            completed_frac  = score_factor(cluster)
            completed_count = int(total_lessons * completed_frac)
            pct             = int(completed_frac * 100)

            cur.execute("""
                INSERT INTO student_progress (
                    total_lessons, completed_lessons, progress_percent,
                    updated_at, enrollment_id
                ) VALUES (%s,%s,%s,%s,%s)
            """, (total_lessons, completed_count, pct, NOW, eid))

            enrollments.append({
                "id":          eid,
                "email":       student["email"],
                "course_id":   cid,
                "enrolled_at": enrolled_at,
                "cluster":     cluster,
            })
            enroll_counter += 1

    conn.commit()
    cur.close()
    conn.close()
    log(f"Inserted {len(enrollments)} enrollments with student_progress")
    return enrollments

# ─────────────────────────────────────────────────────────────────────────────
# STEP 4 — ASSESSMENT-DB: assessments + grades
# ─────────────────────────────────────────────────────────────────────────────
def seed_assessments(students, course_ids, enrollments):
    section("STEP 4 — assessment-db: assessments + grades")
    conn = my("assessment")
    cur  = conn.cursor()

    all_course_ids = [cid for ids in course_ids.values() for cid in ids]

    # 3 assessments per course: QUIZ, PROJECT, EXAM
    assessment_map = {}  # course_id → {type: assessment_id}
    TYPES = [
        ("QUIZ",    "Weekly Quiz",        20.0,  "PUBLISHED"),
        ("PROJECT", "Written Assignment", 50.0,  "PUBLISHED"),
        ("EXAM",    "Final Exam",         100.0, "PUBLISHED"),
    ]

    for cid in all_course_ids:
        assessment_map[cid] = {}
        start = (NOW - timedelta(days=90)).strftime("%Y-%m-%d")
        end   = NOW.strftime("%Y-%m-%d")
        for atype, title_suffix, max_s, status in TYPES:
            cur.execute("""
                INSERT INTO assessment (
                    title, course_name, type, status, class_name,
                    start_date, end_date, duration
                ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s)
            """, (
                f"{title_suffix} (Course #{cid})",
                f"Course {cid}",
                atype, status,
                f"Class-{cid}",
                start, end,
                random.randint(30, 120)
            ))
            assessment_map[cid][atype] = cur.lastrowid

    conn.commit()
    log(f"Inserted {len(all_course_ids) * 3} assessments")

    # Grades: one per student per assessment they're enrolled in
    grade_count = 0
    student_lookup = {s["email"]: s for s in students}

    for enroll in enrollments:
        email   = enroll["email"]
        cid     = enroll["course_id"]
        cluster = enroll["cluster"]
        student = student_lookup.get(email)
        if not student or cid not in assessment_map:
            continue

        graded_at = enroll["enrolled_at"] + timedelta(days=random.randint(30, 60))

        for atype, max_score in [("QUIZ", 20.0), ("PROJECT", 50.0), ("EXAM", 100.0)]:
            aid = assessment_map[cid].get(atype)
            if not aid:
                continue

            # Add noise to score per assessment type
            base_factor = score_factor(cluster)
            noise       = random.uniform(-0.08, 0.08)
            factor      = max(0.05, min(1.0, base_factor + noise))
            raw_score   = round(factor * max_score, 1)

            cur.execute("""
                INSERT INTO grade (
                    assessment_id, student_name, student_email,
                    score, max_score, comments, graded_at
                ) VALUES (%s,%s,%s,%s,%s,%s,%s)
            """, (
                aid,
                student["name"],
                email,
                raw_score,
                max_score,
                random.choice(["Good work", "Needs improvement", "Excellent", "Keep practicing", None]),
                graded_at,
            ))
            grade_count += 1

    conn.commit()
    cur.close()
    conn.close()
    log(f"Inserted {grade_count} grade records")
    return assessment_map

# ─────────────────────────────────────────────────────────────────────────────
# STEP 5 — ATTENDANCE-DB
# ─────────────────────────────────────────────────────────────────────────────
def seed_attendance(students, enrollments):
    section("STEP 5 — attendance-db: attendances")
    conn = pg("attendance")
    cur  = conn.cursor()

    batch   = []
    total   = 0
    est     = len(enrollments) * SESSIONS_PER_ENROLL
    step_start(f"Building ~{est:,} attendance records")

    for enroll in enrollments:
        email      = enroll["email"]
        cid        = enroll["course_id"]
        cluster    = enroll["cluster"]
        ar         = absence_rate(cluster)
        start_date = enroll["enrolled_at"].date()

        for s in range(SESSIONS_PER_ENROLL):
            session_date = start_date + timedelta(days=s * 3)
            if session_date > TODAY:
                break
            r = random.random()
            if r < ar:
                status = "ABSENT"
            elif r < ar + 0.05:
                status = "LATE"
            else:
                status = "PRESENT"
            batch.append((session_date, status, email, cid))

        # flush every 2000 rows to avoid huge memory spikes
        if len(batch) >= 2000:
            psycopg2.extras.execute_values(cur, """
                INSERT INTO attendances (date, status, student_email, course_id)
                VALUES %s
            """, batch, page_size=500)
            conn.commit()
            total += len(batch)
            batch  = []

    if batch:
        psycopg2.extras.execute_values(cur, """
            INSERT INTO attendances (date, status, student_email, course_id)
            VALUES %s
        """, batch, page_size=500)
        conn.commit()
        total += len(batch)

    cur.close()
    conn.close()
    step_done(f"{total:,} rows")

# ─────────────────────────────────────────────────────────────────────────────
# STEP 6 — PAYMENT-DB
# ─────────────────────────────────────────────────────────────────────────────
def seed_payments(enrollments):
    section("STEP 6 — payment-db: payment_entity")
    conn = my("payment")
    cur  = conn.cursor()

    payment_count = 0
    COURSE_PRICES = {cid: random.choice([300.0, 350.0, 400.0, 450.0, 500.0, 550.0, 600.0])
                     for cid in range(1, 30)}

    for enroll in enrollments:
        cluster     = enroll["cluster"]
        enrolled_at = enroll["enrolled_at"]
        delay       = payment_delay_days(cluster)
        status      = payment_status(cluster)
        pay_date    = enrolled_at + timedelta(days=delay)
        if pay_date > NOW:
            pay_date = NOW
        amount      = COURSE_PRICES.get(enroll["course_id"], 400.0)

        cur.execute("""
            INSERT INTO payment_entity (
                amount, method, status, date,
                student_email, course_id, enrollment_id
            ) VALUES (%s,%s,%s,%s,%s,%s,%s)
        """, (
            amount,
            random.choice(["CARD", "CASH", "TRANSFER"]),
            status,
            pay_date,
            enroll["email"],
            enroll["course_id"],
            enroll["id"],
        ))
        payment_count += 1

        # Add a second payment attempt for failed cluster-2 students
        if cluster == 2 and status == "FAILED" and random.random() < 0.5:
            retry_date = pay_date + timedelta(days=random.randint(7, 20))
            cur.execute("""
                INSERT INTO payment_entity (
                    amount, method, status, date,
                    student_email, course_id, enrollment_id
                ) VALUES (%s,%s,%s,%s,%s,%s,%s)
            """, (
                amount,
                random.choice(["CARD", "CASH"]),
                random.choice(["PAID", "PENDING"]),
                retry_date,
                enroll["email"],
                enroll["course_id"],
                enroll["id"],
            ))
            payment_count += 1

    conn.commit()
    cur.close()
    conn.close()
    log(f"Inserted {payment_count} payment records")

# ─────────────────────────────────────────────────────────────────────────────
# STEP 7 — SCHEDULE-DB
# ─────────────────────────────────────────────────────────────────────────────
def seed_schedules(course_ids, class_ids):
    section("STEP 7 — schedule-db: schedules")
    conn = pg("schedule")
    cur  = conn.cursor()

    DAYS     = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
    ROOMS    = ["Room 101", "Room 102", "Room 201", "Room 202", "Lab A", "Lab B"]
    SLOTS    = [(time(8, 0), time(10, 0)), (time(10, 0), time(12, 0)),
                (time(14, 0), time(16, 0)), (time(16, 0), time(18, 0))]
    count    = 0

    LEVEL_TO_CLASSES = {
        "L1": ["L1-A", "L1-B"],
        "L2": ["L2-A"],
        "L3": ["L3-A"],
        "M1": ["M1-A"],
        "M2": ["M2-A"],
    }

    for level, cids in course_ids.items():
        classes_for_level = LEVEL_TO_CLASSES.get(level, [f"{level}-A"])
        for i, cid in enumerate(cids):
            for cname in classes_for_level:
                day   = DAYS[count % len(DAYS)]
                slot  = SLOTS[count % len(SLOTS)]
                room  = ROOMS[count % len(ROOMS)]
                cur.execute("""
                    INSERT INTO schedules (day_of_week, start_time, end_time, room, course_id, class_name)
                    VALUES (%s,%s,%s,%s,%s,%s)
                """, (day, slot[0], slot[1], room, cid, cname))
                count += 1

    conn.commit()
    cur.close()
    conn.close()
    log(f"Inserted {count} schedule entries")

# ─────────────────────────────────────────────────────────────────────────────
# STEP 8 — REPORTING-DB: reclamations (optional, not used for ML)
# ─────────────────────────────────────────────────────────────────────────────
def seed_reporting(students):
    section("STEP 8 — reporting-db: reclamations")
    conn = my("reporting")
    cur  = conn.cursor()

    CATEGORIES = ["Technical", "Administrative", "Academic", "Financial", "Other"]
    PRIORITIES  = ["LOW", "MEDIUM", "HIGH"]
    STATUSES    = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"]
    SUBJECTS    = [
        "Grade dispute",
        "Attendance mark correction",
        "Payment not reflected",
        "Access to course material",
        "Schedule conflict",
        "Technical issue with platform",
    ]

    count = 0
    # Only at-risk students file more complaints
    for student in students:
        if student["cluster"] == 2:
            n = random.randint(1, 3)
        elif student["cluster"] == 1:
            n = random.randint(0, 1)
        else:
            n = 0

        for _ in range(n):
            created = NOW - timedelta(days=random.randint(1, 90))
            cur.execute("""
                INSERT INTO reclamation (
                    subject, message, student_email,
                    category, priority, status,
                    created_date, updated_date
                ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s)
            """, (
                random.choice(SUBJECTS),
                fake.paragraph(nb_sentences=2),
                student["email"],
                random.choice(CATEGORIES),
                random.choice(PRIORITIES),
                random.choice(STATUSES),
                created, created + timedelta(days=random.randint(0, 10))
            ))
            count += 1

    conn.commit()
    cur.close()
    conn.close()
    log(f"Inserted {count} reclamation records")

# ─────────────────────────────────────────────────────────────────────────────
# STEP 9 — RESOURCES-DB (optional, not used for ML)
# ─────────────────────────────────────────────────────────────────────────────
def seed_resources(assessment_map):
    section("STEP 9 — resources-db: learning_resource")
    conn = my("resources")
    cur  = conn.cursor()

    count = 0
    TYPES = ["PDF", "VIDEO", "DOCUMENT", "LINK"]
    for cid, type_map in assessment_map.items():
        for atype, aid in type_map.items():
            cur.execute("""
                INSERT INTO learning_resource (
                    title, type, published, assessment_id,
                    file_url, uploaded_at
                ) VALUES (%s,%s,%s,%s,%s,%s)
            """, (
                f"Study Guide — {atype} (Course {cid})",
                random.choice(TYPES),
                True,
                aid,
                f"https://cdn.esm.edu/resources/course{cid}_{atype.lower()}.pdf",
                NOW,
            ))
            count += 1

    conn.commit()
    cur.close()
    conn.close()
    log(f"Inserted {count} learning resources")

# ─────────────────────────────────────────────────────────────────────────────
# MAIN
# ─────────────────────────────────────────────────────────────────────────────
def main():
    print("\n" + "═" * 60)
    print("  ESM — Demo Data Generator")
    print("  English School Management · ML Pipeline")
    print("═" * 60)

    if RESET:
        reset_all()

    students, class_ids  = seed_auth()
    course_ids           = seed_courses()
    enrollments          = seed_enrollments(students, course_ids)
    assessment_map       = seed_assessments(students, course_ids, enrollments)
    seed_attendance(students, enrollments)
    seed_payments(enrollments)
    seed_schedules(course_ids, class_ids)
    seed_reporting(students)
    seed_resources(assessment_map)

    print("\n" + "═" * 58)
    print("  Done. Summary:")
    print(f"  • {len(students)} students across 6 classes (3 ML clusters)")
    print(f"  • {len(enrollments)} enrollments across 12 courses")
    print(f"  • ~{len(enrollments)*25:,} attendance records")
    print(f"  • ~{len(enrollments)*3:,} grade records")
    print(f"  • ~{len(enrollments):,} payment records")
    print("═" * 58)
    print("\n  Next step → POST http://localhost:8100/ml/dataset/build\n")


if __name__ == "__main__":
    main()