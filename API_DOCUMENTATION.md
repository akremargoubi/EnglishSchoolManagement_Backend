# API Documentation — Assessment & Resources Services

All requests go through the gateway at **`http://localhost:8080`**.  
Every request must carry the JWT header:

```
Authorization: Bearer <token>
```

The gateway extracts `callerId` (UUID), `callerRole` (STUDENT | TUTOR | ADMIN), and `callerEmail` from the token and forwards them to each service automatically.

> **ADMIN is always unrestricted.** Every endpoint listed below is fully accessible to ADMIN regardless of ownership, class assignment, or any other constraint. The access notes below only describe the additional rules that apply to TUTOR and STUDENT roles.

---

## Table of Contents

1. [Assessment Service](#1-assessment-service)
   - [Assessments](#11-assessments)
   - [Grades](#12-grades)
   - [Notifications](#13-notifications)
   - [Planning / Calendar](#14-planning--calendar)
   - [Enums](#15-enums)
2. [Resources Service](#2-resources-service)
3. [Data Models](#3-data-models)
4. [Role-Based Access Reference](#4-role-based-access-reference)
5. [Auto-Triggered Behaviours](#5-auto-triggered-behaviours)
6. [Common Flows](#6-quick-reference-common-flows)

---

## 1. Assessment Service

Internal port 8081, gateway path prefix `/api/assessments`, `/api/grades`, `/api/notifications`, `/api/planning`, `/api/enums`.

---

### 1.1 Assessments

**Base path:** `/api/assessments`

#### `POST /api/assessments` — Create assessment

> **ADMIN** — unrestricted.  
> **TUTOR** — allowed.  
> **STUDENT** — 403 Forbidden.

**Request body**
```json
{
  "title": "Midterm Exam",
  "courseName": "English Grammar",
  "type": "EXAM",
  "status": "DRAFT",
  "className": "TWIN1",
  "startDate": "2026-05-10",
  "endDate": "2026-05-10",
  "duration": 90
}
```

**Response** — `200 OK` — [Assessment object](#assessment)

---

#### `GET /api/assessments` — List all assessments

> **ADMIN** — returns all assessments.  
> **TUTOR** — returns only assessments for their own classes.  
> **STUDENT** — 403 Forbidden.

**Response** — `200 OK` — `Assessment[]`

---

#### `GET /api/assessments/{id}` — Get by ID

**Path param:** `id` (number)

**Response** — `200 OK` — [Assessment object](#assessment)

---

#### `GET /api/assessments/class/{className}` — Get by class name

**Path param:** `className` e.g. `TWIN1`

> **ADMIN** — all assessments in the class.  
> **TUTOR** — if assigned to that class.  
> **STUDENT** — if enrolled in that class.

**Response** — `200 OK` — `Assessment[]`

---

#### `PUT /api/assessments/{id}` — Update assessment

**Path param:** `id` (number)

> **ADMIN** — unrestricted.  
> **TUTOR** — creator of the assessment only.  
> **STUDENT** — 403 Forbidden.

**Request body** — all fields optional, send only what changes:
```json
{
  "status": "PUBLISHED",
  "endDate": "2026-05-12"
}
```

**Response** — `200 OK` — [Assessment object](#assessment)

---

#### `DELETE /api/assessments/{id}` — Delete assessment

**Path param:** `id` (number)

> **ADMIN** — unrestricted.  
> **TUTOR** — creator only.  
> **STUDENT** — 403 Forbidden.

**Response** — `204 No Content`

---

### 1.2 Grades

**Base path:** `/api/grades`

#### `POST /api/grades` — Grade a student

> **ADMIN** — unrestricted.  
> **TUTOR** — must be assigned to the assessment's class.  
> **STUDENT** — 403 Forbidden.

**Request body**
```json
{
  "assessmentId": 12,
  "studentName": "Akrem Argoubi",
  "studentEmail": "student@example.com",
  "score": 17.5,
  "maxScore": 20,
  "comments": "Good work on the listening section."
}
```

**Response** — `200 OK` — [Grade object](#grade)

> **Side effect:** if `score / maxScore >= 0.5` the Certificate Service is automatically notified.

---

#### `GET /api/grades` — All grades

> **ADMIN** — all grades in the system.  
> **TUTOR / STUDENT** — 403 Forbidden.

**Response** — `200 OK` — `Grade[]`

---

#### `GET /api/grades/{id}` — Get grade by ID

**Response** — `200 OK` — [Grade object](#grade)

---

#### `GET /api/grades/assessment/{assessmentId}` — Grades for an assessment

**Path param:** `assessmentId` (number)

> **ADMIN** — unrestricted.  
> **TUTOR** — own class only.  
> **STUDENT** — 403 Forbidden.

**Response** — `200 OK` — `Grade[]`

---

#### `GET /api/grades/student/{email}` — Grades for a student

**Path param:** `email` (URL-encoded string)

> **ADMIN** — any student.  
> **TUTOR** — students in their own classes.  
> **STUDENT** — own grades only (email must match their own).

**Response** — `200 OK` — `Grade[]`

---

#### `GET /api/grades/assessment/{assessmentId}/stats` — Assessment statistics

**Path param:** `assessmentId` (number)

> **ADMIN** — unrestricted.  
> **TUTOR** — own class only.  
> **STUDENT** — 403 Forbidden.

**Response**
```json
{
  "total": 24,
  "average": 14.8,
  "max": 20.0,
  "min": 6.5,
  "passing": 20,
  "failing": 4,
  "passRate": 83.33
}
```

---

#### `GET /api/grades/leaderboard` — Global leaderboard

> **ADMIN** — unrestricted.  
> **TUTOR / STUDENT** — 403 Forbidden.

Ordered by `averagePercentage` descending.

**Response**
```json
[
  {
    "rank": 1,
    "studentName": "Akrem Argoubi",
    "studentEmail": "student@example.com",
    "averagePercentage": 92.5,
    "averageScore": 18.5,
    "totalAssessments": 6,
    "mention": "EXCELLENT"
  }
]
```

---

#### `GET /api/grades/leaderboard/assessment/{assessmentId}` — Per-assessment leaderboard

**Path param:** `assessmentId` (number)

> **ADMIN** — unrestricted.  
> **TUTOR** — own class only.  
> **STUDENT** — 403 Forbidden.

Ordered by `score` descending.

**Response**
```json
[
  {
    "rank": 1,
    "studentName": "Akrem Argoubi",
    "studentEmail": "student@example.com",
    "score": 19.0,
    "maxScore": 20.0,
    "percentage": 95.0,
    "mention": "EXCELLENT",
    "comments": "Outstanding."
  }
]
```

---

#### `PUT /api/grades/{id}` — Update a grade

**Path param:** `id` (number)

> **ADMIN** — unrestricted.  
> **TUTOR** — original grader only.  
> **STUDENT** — 403 Forbidden.

**Request body** — partial, same fields as create:
```json
{
  "score": 18.0,
  "comments": "Revised after appeal."
}
```

**Response** — `200 OK` — [Grade object](#grade)

---

#### `DELETE /api/grades/{id}` — Delete a grade

**Path param:** `id` (number)

> **ADMIN** — unrestricted.  
> **TUTOR** — original grader only.  
> **STUDENT** — 403 Forbidden.

**Response** — `204 No Content`

---

### 1.3 Notifications

**Base path:** `/api/notifications`

All authenticated roles can access their own notifications. Each user sees:
- Notifications where `targetEmail` matches their JWT email
- Broadcast notifications where `targetEmail` is `null`

#### `GET /api/notifications` — All notifications for the caller

**Response** — `200 OK` — `Notification[]` ordered newest first

---

#### `GET /api/notifications/unread` — Unread notifications

**Response** — `200 OK` — `Notification[]` ordered newest first

---

#### `GET /api/notifications/count` — Unread badge count

**Response**
```json
{ "count": 3 }
```

---

#### `PUT /api/notifications/{id}/read` — Mark one as read

**Path param:** `id` (number)

**Response** — `200 OK` — [Notification object](#notification)

---

#### `PUT /api/notifications/read-all` — Mark all as read

**Response** — `200 OK`

---

#### `DELETE /api/notifications/{id}` — Delete notification

**Path param:** `id` (number)

**Response** — `204 No Content`

---

### 1.4 Planning / Calendar

**Base path:** `/api/planning`

All planning endpoints return `Assessment[]`. Accessible to any authenticated role.

#### `GET /api/planning/calendar?year=2026&month=5` — Monthly calendar view

| Query param | Type | Default | Description |
|---|---|---|---|
| `year` | int | current year | 4-digit year |
| `month` | int | current month | 1–12 |

**Response** — `Assessment[]` whose `startDate` falls within the given month

---

#### `GET /api/planning/upcoming` — Upcoming assessments

Returns assessments where `startDate` is after today, ordered by `startDate` ascending.

**Response** — `Assessment[]`

---

#### `GET /api/planning/ongoing` — Ongoing assessments

Returns assessments where `startDate <= today <= endDate`.

**Response** — `Assessment[]`

---

#### `GET /api/planning/range?start=…&end=…` — Assessments in a date range

| Query param | Format | Example |
|---|---|---|
| `start` | ISO DateTime | `2026-05-01T00:00:00` |
| `end` | ISO DateTime | `2026-05-31T23:59:59` |

**Response** — `Assessment[]` ordered by `startDate` ascending

---

### 1.5 Enums

**Base path:** `/api/enums`

Use these to populate dropdowns — they always reflect the exact values the backend accepts.

#### `GET /api/enums/types`
```json
["EXAM", "QUIZ", "PROJECT"]
```

#### `GET /api/enums/statuses`
```json
["DRAFT", "PUBLISHED", "CLOSED"]
```

---

## 2. Resources Service

Internal port 8096, gateway path prefix `/api/resources`.

**Base path:** `/api/resources`

#### `POST /api/resources/upload` — Upload a resource file

> **ADMIN** — unrestricted.  
> **TUTOR** — must be the assigned tutor of the assessment's class.  
> **STUDENT** — 403 Forbidden.

**Content-Type:** `multipart/form-data`

| Field | Type | Required | Notes |
|---|---|---|---|
| `title` | string | ✓ | Display name shown to students |
| `type` | string | ✓ | `PDF`, `VIDEO`, `AUDIO`, `IMAGE`, or any custom label |
| `published` | boolean | ✓ | `true` = visible to enrolled students |
| `assessmentId` | number | ✓ | ID of the linked assessment |
| `file` | File | ✓ | Max **20 MB** |

**Response** — `200 OK` — [LearningResource object](#learningresource)

---

#### `GET /api/resources/assessment/{assessmentId}` — List resources for an assessment

**Path param:** `assessmentId` (number)

> **ADMIN** — all resources, published or not.  
> **TUTOR** — all resources if assigned to that assessment's class.  
> **STUDENT** — published resources only (`published: true`) if enrolled in the class.  
> Other — 403 Forbidden.

**Response** — `200 OK` — `LearningResource[]`

---

#### `DELETE /api/resources/{id}` — Delete a resource

**Path param:** `id` (number)

> **ADMIN** — unrestricted.  
> **TUTOR** — original uploader only.  
> **STUDENT** — 403 Forbidden.

**Response** — `204 No Content`

---

## 3. Data Models

### Assessment

```json
{
  "id": 1,
  "title": "Midterm Exam",
  "courseName": "English Grammar",
  "type": "EXAM",
  "status": "PUBLISHED",
  "className": "TWIN1",
  "startDate": "2026-05-10",
  "endDate": "2026-05-10",
  "duration": 90,
  "tutorId": "uuid-string"
}
```

| Field | Type | Notes |
|---|---|---|
| `id` | number | Auto-generated |
| `title` | string | |
| `courseName` | string | |
| `type` | `"EXAM"` \| `"QUIZ"` \| `"PROJECT"` | |
| `status` | `"DRAFT"` \| `"PUBLISHED"` \| `"CLOSED"` | |
| `className` | string \| null | e.g. `"TWIN1"`, `"DS3"` |
| `startDate` | string | `"YYYY-MM-DD"` |
| `endDate` | string | `"YYYY-MM-DD"` |
| `duration` | number \| null | Minutes |
| `tutorId` | string (UUID) | Auto-set from JWT on create |

---

### Grade

```json
{
  "id": 42,
  "assessmentId": 12,
  "studentName": "Akrem Argoubi",
  "studentEmail": "student@example.com",
  "score": 17.5,
  "maxScore": 20.0,
  "percentage": 87.5,
  "mention": "GOOD",
  "comments": "Good work.",
  "gradedByTutorId": "uuid-string",
  "gradedAt": "2026-05-11T14:30:00"
}
```

| Field | Type | Notes |
|---|---|---|
| `id` | number | |
| `assessmentId` | number | |
| `studentName` | string | |
| `studentEmail` | string | |
| `score` | number | |
| `maxScore` | number | |
| `percentage` | number | `(score/maxScore)*100` — computed, read-only |
| `mention` | string | Computed — see table below |
| `comments` | string \| null | |
| `gradedByTutorId` | string (UUID) | Auto-set from JWT |
| `gradedAt` | string (ISO datetime) | Auto-set on creation |

**Mention thresholds**

| `percentage` | `mention` |
|---|---|
| ≥ 90 | `EXCELLENT` |
| ≥ 75 | `GOOD` |
| ≥ 60 | `AVERAGE` |
| < 60 | `FAIL` |

**Passing grade:** `score >= maxScore * 0.5`

---

### Notification

```json
{
  "id": 7,
  "type": "ASSESSMENT_CREATED",
  "title": "New assessment available",
  "message": "Midterm Exam has been published for TWIN1.",
  "targetEmail": "student@example.com",
  "read": false,
  "createdAt": "2026-05-09T09:00:00",
  "relatedId": 12
}
```

| Field | Type | Notes |
|---|---|---|
| `id` | number | |
| `type` | `"ASSESSMENT_CREATED"` \| `"RESOURCE_ADDED"` | |
| `title` | string | |
| `message` | string | max 500 chars |
| `targetEmail` | string \| **null** | `null` = broadcast to all users |
| `read` | boolean | |
| `createdAt` | string (ISO datetime) | |
| `relatedId` | number | linked assessment or resource `id` |

---

### LearningResource

```json
{
  "id": 3,
  "title": "Grammar Notes",
  "type": "PDF",
  "published": true,
  "assessmentId": 12,
  "fileUrl": "uploads/1746000000000_grammar-notes.pdf",
  "uploadedBy": "uuid-string",
  "uploadedAt": "2026-05-09T10:15:00Z"
}
```

| Field | Type | Notes |
|---|---|---|
| `id` | number | |
| `title` | string | |
| `type` | string | Free-form label |
| `published` | boolean | `false` = hidden from students |
| `assessmentId` | number | |
| `fileUrl` | string | Relative server path |
| `uploadedBy` | string (UUID) | Auto-set from JWT |
| `uploadedAt` | string (ISO datetime) | Auto-set |

---

## 4. Role-Based Access Reference

> **ADMIN always has full access** — the ✅ / restricted / ❌ column for ADMIN is always ✅ unrestricted.

| Endpoint | ADMIN | TUTOR | STUDENT |
|---|---|---|---|
| Create assessment | ✅ | ✅ | ❌ |
| List all assessments | ✅ all | ✅ own classes only | ❌ |
| Get assessment by class | ✅ | ✅ if assigned | ✅ if enrolled |
| Update assessment | ✅ | ✅ creator only | ❌ |
| Delete assessment | ✅ | ✅ creator only | ❌ |
| Create grade | ✅ | ✅ own class | ❌ |
| List all grades (`GET /api/grades`) | ✅ | ❌ | ❌ |
| Grades by assessment | ✅ | ✅ own class | ❌ |
| Grades by student | ✅ | ✅ own students | ✅ own only |
| Update grade | ✅ | ✅ original grader | ❌ |
| Delete grade | ✅ | ✅ original grader | ❌ |
| Assessment stats | ✅ | ✅ own class | ❌ |
| Per-assessment leaderboard | ✅ | ✅ own class | ❌ |
| Global leaderboard | ✅ | ❌ | ❌ |
| View own notifications | ✅ | ✅ | ✅ |
| Planning endpoints | ✅ | ✅ | ✅ |
| Upload resource | ✅ | ✅ assigned tutor | ❌ |
| List resources | ✅ all | ✅ all (own class) | ✅ published only |
| Delete resource | ✅ | ✅ original uploader | ❌ |

---

## 5. Auto-Triggered Behaviours

The frontend does **not** need to call any extra endpoint for these — they happen server-side.

| Trigger | What the backend does automatically |
|---|---|
| Assessment updated to `status: "PUBLISHED"` | Creates a notification for every student in `className` |
| Resource uploaded with `published: true` | Creates a notification for the assessment's class |
| Grade created and `score >= maxScore * 0.5` | Certificate Service is notified asynchronously |

---

## 6. Quick-Reference: Common Flows

### Notification bell (any role)
```
GET /api/notifications/count          → { "count": N } for badge
GET /api/notifications/unread         → list for dropdown
PUT /api/notifications/{id}/read      → mark one read on click
PUT /api/notifications/read-all       → clear all button
```

### Tutor publishes an assessment
```
PUT /api/assessments/{id}
body: { "status": "PUBLISHED" }
→ backend auto-notifies all students in the class
```

### Admin or tutor views class performance
```
GET /api/grades/assessment/{assessmentId}/stats
GET /api/grades/leaderboard/assessment/{assessmentId}
```

### Admin views global leaderboard
```
GET /api/grades/leaderboard
```

### Tutor uploads a study document
```
POST /api/resources/upload
multipart: title, type, published=true, assessmentId, file
```

### Student opens an assessment's materials
```
GET /api/resources/assessment/{assessmentId}
→ returns published resources only
```

### Student views own grades
```
GET /api/grades/student/{studentEmail}
```

### Calendar / planning view
```
GET /api/planning/calendar?year=2026&month=5    → month grid
GET /api/planning/upcoming                       → next assessments list
GET /api/planning/ongoing                        → in-progress now
GET /api/planning/range?start=…&end=…           → custom range
```
