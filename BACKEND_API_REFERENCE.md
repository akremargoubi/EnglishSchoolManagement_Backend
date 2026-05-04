# ESM Platform — Full Backend API Reference

> **For frontend agents**: All requests go through the API Gateway at `http://localhost:8080`.
> Every path below is a **gateway path** — do not call service ports directly.
> Auth header: `Authorization: Bearer <jwt_token>` (7-day token, returned by login/register).

---

## Base URL

```
http://localhost:8080
```

---

## Authentication & JWT

JWT claims include:
- `sub` — UUID (string) of the user (used as `userId`)
- `role` — one of: `ADMIN`, `STUDENT`, `TUTOR`, `PARENT`
- `email` — user's email address

> **ADMIN is always unrestricted** on all endpoints unless noted. Role restrictions described below apply to TUTOR and STUDENT only.

---

## 1. Auth Service — `/api/auth`, `/api/users`, `/api/classes`, `/api/parents`

Internal port: **1999** | DB: **PostgreSQL**

### 1.1 Authentication

| Method | Path | Body | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | `RegisterRequest` | Register new user, returns JWT |
| POST | `/api/auth/login` | `AuthRequest` | Login, returns JWT (or 2FA challenge) |
| POST | `/api/auth/2fa/verify` | `{ email, code }` | Complete 2FA login, returns JWT |
| POST | `/api/auth/2fa/enable` | — (Bearer token) | Enable 2FA for current user |
| POST | `/api/auth/2fa/disable` | — (Bearer token) | Disable 2FA |
| POST | `/api/auth/email/send-verification` | `{ email }` | Send email verification link |
| POST | `/api/auth/email/verify` | `{ token }` | Verify email with token |
| POST | `/api/auth/password/reset-request` | `{ email }` | Send password reset email |
| POST | `/api/auth/password/reset-confirm` | `{ token, newPassword }` | Confirm password reset |

**RegisterRequest body:**
```json
{
  "firstName": "Alice",
  "lastName": "Smith",
  "email": "alice@example.com",
  "password": "Secret123!",
  "role": "STUDENT",
  "cin": "12345678",
  "phoneNumber": "+21612345678"
}
```
Role values: `ADMIN` `STUDENT` `TUTOR` `PARENT`

**Login response (AuthFlowResponse):**
```json
{
  "token": "eyJ...",
  "requiresTwoFactor": false,
  "userId": "uuid-string",
  "role": "STUDENT"
}
```

---

### 1.2 Users

| Method | Path | Params / Body | Description |
|--------|------|---------------|-------------|
| GET | `/api/users/me` | Bearer token | Get current user profile |
| PUT | `/api/users/me` | `UserSelfUpdateRequest` | Update own profile (no role change) |
| POST | `/api/users/me/avatar` | multipart `file` | Upload avatar → S3/MinIO |
| GET | `/api/users` | see below | Search / filter users (paginated) |
| GET | `/api/users/{id}` | — | Get user by UUID |
| POST | `/api/users` | `UserCreateRequest` | Create user (Admin) |
| PUT | `/api/users/{id}` | `UserUpdateRequest` | Update user (Admin) |
| DELETE | `/api/users/{id}` | — | Soft-delete user (Admin) |

**GET /api/users — query parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `role` | string | Filter by role: `STUDENT`, `TUTOR`, `ADMIN`, `PARENT` |
| `email` | string | Partial match (case-insensitive) |
| `firstName` | string | Partial match (case-insensitive) |
| `lastName` | string | Partial match (case-insensitive) |
| `cin` | string | Partial match |
| `phoneNumber` | string | Partial match |
| `page` | int | 0-based page index |
| `size` | int | Page size |

> When `role` is provided the query uses `WHERE role = ? AND deleted_at IS NULL` — soft-deleted users are excluded. When role is omitted, the text-search query runs (does not filter deleted users currently).

**Wallet & Parent endpoints:**

| Method | Path | Body | Description |
|--------|------|------|-------------|
| GET | `/api/users/{id}/wallet` | — | `{ userId, walletBalance }` |
| PUT | `/api/users/{id}/wallet/topup` | `{ "amount": 100.0 }` | Add funds to wallet |
| PUT | `/api/users/{id}/wallet/deduct` | `{ "amount": 50.0 }` | Deduct funds (used internally by enrollment) |
| PUT | `/api/users/{id}/parent-email` | `{ "parentEmail": "p@email.com" }` | Link parent — triggers invite email |

**UserResponseDto:**
```json
{
  "id": "uuid",
  "uuid": "uuid-string",
  "firstName": "Alice",
  "lastName": "Smith",
  "email": "alice@example.com",
  "role": "STUDENT",
  "cin": "12345678",
  "phoneNumber": "+21612345678",
  "address": "...",
  "avatarUrl": "https://esms3.dominnovate.com/...",
  "status": "ACTIVE",
  "emailVerified": true,
  "twoFactorEnabled": false,
  "walletBalance": 250.0,
  "parentEmail": "parent@email.com",
  "parentId": "uuid-or-null",
  "classId": 1,
  "className": "TWIN1",
  "classLevel": "L3",
  "classSpecialty": "TWIN",
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-04-01T00:00:00Z"
}
```
> `className` is `null` (JSON null, never `""`) when the student is not enrolled in any class.

---

### 1.3 Classes

| Method | Path | Body | Description |
|--------|------|------|-------------|
| GET | `/api/classes` | — | Get all classes |
| GET | `/api/classes/{id}` | — | Get class by ID |
| POST | `/api/classes` | `StudentClassRequest` | Create class |
| PUT | `/api/classes/{id}` | `StudentClassRequest` | Update class |
| DELETE | `/api/classes/{id}` | — | Delete class (unlinks all students first) |
| GET | `/api/classes/{classId}/students` | — | List students in class |
| PUT | `/api/classes/{classId}/students/{userId}` | — | Enroll student into class |
| DELETE | `/api/classes/{classId}/students/{userId}` | — | Remove student from class |
| PUT | `/api/classes/{classId}/tutor/{tutorId}` | — | Assign tutor to class |
| DELETE | `/api/classes/{classId}/tutor` | — | Remove tutor from class |
| GET | `/api/classes/by-tutor/{tutorId}` | — | Classes assigned to a tutor (used internally by assessment-service) |

**StudentClassRequest body:**
```json
{
  "name": "TWIN1",
  "level": "L3",
  "specialty": "TWIN",
  "description": "Software Engineering",
  "tutorId": "uuid-or-null"
}
```

**StudentClassResponseDto:**
```json
{
  "id": 1,
  "name": "TWIN1",
  "level": "L3",
  "specialty": "TWIN",
  "description": "Software Engineering",
  "studentCount": 24,
  "tutorId": "uuid",
  "tutorFirstName": "Bob",
  "tutorLastName": "Tutor",
  "tutorEmail": "bob@school.com",
  "students": [
    { "id": "uuid", "firstName": "Alice", "lastName": "Smith", "email": "...", "cin": "...", "avatarUrl": "..." }
  ],
  "createdAt": "2026-01-01T00:00:00Z"
}
```

> **Enrollment side-effect:** `PUT /api/classes/{classId}/students/{userId}` sets `class_id` on the user row. `GET /api/users?role=STUDENT` immediately reflects the new `className`. Removal reverts `className` to `null`.

---

### 1.4 Parents

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/parents` | List all parents |
| GET | `/api/parents/{id}` | Get parent by ID |
| POST | `/api/parents` | Create parent account |
| PUT | `/api/parents/{id}` | Update parent |
| DELETE | `/api/parents/{id}` | Delete parent |
| GET | `/api/parents/{parentId}/children` | Get children linked to a parent |
| POST | `/api/parents/{parentId}/children/{childId}` | Link child to parent |
| DELETE | `/api/parents/{parentId}/children/{childId}` | Unlink child |

---

## 2. Assessment Service — `/api/assessments`, `/api/grades`, `/api/notifications`, `/api/planning`, `/api/enums`

Internal port: **8081** | DB: **MySQL**

> JWT must be valid (same `JWT_SECRET` as auth-service).  
> `ADMIN` — unrestricted on all endpoints.  
> Role checks use plain string comparison: `"TUTOR"`, `"STUDENT"`, `"ADMIN"` (no `ROLE_` prefix).

### 2.1 Assessments

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/assessments` | ADMIN (all), TUTOR (own classes) | List assessments |
| GET | `/api/assessments/{id}` | All authenticated | Get by ID |
| GET | `/api/assessments/class/{className}` | ADMIN, TUTOR (assigned), STUDENT (enrolled) | Assessments for a class |
| POST | `/api/assessments` | ADMIN, TUTOR | Create assessment |
| PUT | `/api/assessments/{id}` | ADMIN, TUTOR (creator only) | Update assessment |
| DELETE | `/api/assessments/{id}` | ADMIN, TUTOR (creator only) | Delete assessment |

**Assessment body:**
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
`type` values: `EXAM` `QUIZ` `PROJECT`  
`status` values: `DRAFT` `PUBLISHED` `CLOSED`

> `tutorId` is auto-set from the JWT on create.  
> **Side-effect:** changing status to `PUBLISHED` auto-creates notifications for all students in `className`.

---

### 2.2 Grades

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/api/grades` | ADMIN only | All grades in system |
| GET | `/api/grades/{id}` | All authenticated | Get by ID |
| GET | `/api/grades/assessment/{assessmentId}` | ADMIN, TUTOR (own class) | Grades for an assessment |
| GET | `/api/grades/student/{email}` | ADMIN, TUTOR (own students), STUDENT (own only) | Grades for a student |
| GET | `/api/grades/assessment/{assessmentId}/stats` | ADMIN, TUTOR (own class) | Statistics |
| GET | `/api/grades/leaderboard` | ADMIN only | Global leaderboard |
| GET | `/api/grades/leaderboard/assessment/{assessmentId}` | ADMIN, TUTOR (own class) | Per-assessment leaderboard |
| POST | `/api/grades` | ADMIN, TUTOR (own class) | Grade a student |
| PUT | `/api/grades/{id}` | ADMIN, TUTOR (original grader) | Update grade |
| DELETE | `/api/grades/{id}` | ADMIN, TUTOR (original grader) | Delete grade |

**Grade body:**
```json
{
  "assessmentId": 12,
  "studentName": "Alice Smith",
  "studentEmail": "alice@example.com",
  "score": 17.5,
  "maxScore": 20,
  "comments": "Good work."
}
```

**Grade response:**
```json
{
  "id": 42,
  "assessmentId": 12,
  "studentName": "Alice Smith",
  "studentEmail": "alice@example.com",
  "score": 17.5,
  "maxScore": 20.0,
  "percentage": 87.5,
  "mention": "GOOD",
  "comments": "Good work.",
  "gradedByTutorId": "uuid",
  "gradedAt": "2026-05-11T14:30:00"
}
```

**Mention thresholds:** ≥90 → `EXCELLENT` | ≥75 → `GOOD` | ≥60 → `AVERAGE` | <60 → `FAIL`

**Passing grade:** `score >= maxScore * 0.5`

> **Side-effect:** Creating a passing grade triggers an asynchronous call to the Certificate Service (Python FastAPI at port 8097).

**Stats response:**
```json
{ "total": 24, "average": 14.8, "max": 20.0, "min": 6.5, "passing": 20, "failing": 4, "passRate": 83.33 }
```

**Leaderboard entry:**
```json
{ "rank": 1, "studentName": "Alice", "studentEmail": "alice@example.com", "averagePercentage": 92.5, "averageScore": 18.5, "totalAssessments": 6, "mention": "EXCELLENT" }
```

---

### 2.3 Notifications

Each user sees their own notifications + broadcasts (`targetEmail == null`).

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/notifications` | All notifications (newest first) |
| GET | `/api/notifications/unread` | Unread only |
| GET | `/api/notifications/count` | `{ "count": N }` |
| PUT | `/api/notifications/{id}/read` | Mark one read |
| PUT | `/api/notifications/read-all` | Mark all read |
| DELETE | `/api/notifications/{id}` | Delete notification |

**Notification shape:**
```json
{
  "id": 7,
  "type": "ASSESSMENT_CREATED",
  "title": "New assessment",
  "message": "Midterm Exam published for TWIN1.",
  "targetEmail": "alice@example.com",
  "read": false,
  "createdAt": "2026-05-09T09:00:00",
  "relatedId": 12
}
```
`type` values: `ASSESSMENT_CREATED` `RESOURCE_ADDED`  
`targetEmail: null` = broadcast to all users.

---

### 2.4 Planning / Calendar

All planning endpoints are open to any authenticated role.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/planning/calendar?year=2026&month=5` | Assessments in a given month |
| GET | `/api/planning/upcoming` | Future assessments (startDate > today), asc |
| GET | `/api/planning/ongoing` | Current assessments (startDate ≤ today ≤ endDate) |
| GET | `/api/planning/range?start=2026-05-01T00:00:00&end=2026-05-31T23:59:59` | Assessments in date range |

---

### 2.5 Enums

| Method | Path | Response |
|--------|------|----------|
| GET | `/api/enums/types` | `["EXAM","QUIZ","PROJECT"]` |
| GET | `/api/enums/statuses` | `["DRAFT","PUBLISHED","CLOSED"]` |

---

## 3. Resources Service — `/api/resources`

Internal port: **8096** | DB: **MySQL**

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| POST | `/api/resources/upload` | ADMIN, TUTOR (assigned) | Upload resource file |
| GET | `/api/resources/assessment/{assessmentId}` | ADMIN/TUTOR (all), STUDENT (published only) | List resources |
| DELETE | `/api/resources/{id}` | ADMIN, TUTOR (uploader) | Delete resource |

**Upload — multipart/form-data fields:**

| Field | Type | Notes |
|-------|------|-------|
| `title` | string | Display name |
| `type` | string | `PDF`, `VIDEO`, `AUDIO`, `IMAGE`, or custom label |
| `published` | boolean | `true` = visible to enrolled students |
| `assessmentId` | number | Linked assessment ID |
| `file` | File | Max **20 MB** |

**LearningResource response:**
```json
{
  "id": 3,
  "title": "Grammar Notes",
  "type": "PDF",
  "published": true,
  "assessmentId": 12,
  "fileUrl": "uploads/1746000000000_grammar-notes.pdf",
  "uploadedBy": "uuid",
  "uploadedAt": "2026-05-09T10:15:00Z"
}
```

> **Side-effect:** Uploading a published resource auto-creates a notification for the assessment's class.

---

## 4. Course Service — `/api/v1/courses`, `/api/v1/modules`, `/api/v1/lessons`, `/api/v1/categories`, `/api/v1/instructors`, `/api/v1/reviews`, `/api/v1/upload`

Internal port: **8086** | DB: **PostgreSQL** (Docker) / H2 in-memory (local dev)

> No JWT enforcement — all endpoints are open (SecurityConfig permits all).

### 4.1 Courses

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/courses` | Search / list with filters (see below) |
| GET | `/api/v1/courses/{id}` | Get course by ID |
| POST | `/api/v1/courses` | Create course |
| PUT | `/api/v1/courses/{id}` | Update course |
| PATCH | `/api/v1/courses/{id}/publish` | Publish course (`isPublished = true`) |
| DELETE | `/api/v1/courses/{id}` | Delete course |
| PUT | `/api/v1/courses/{id}/assign-tutor` | `{ "tutorEmail": "tutor@school.com" }` |
| GET | `/api/v1/courses/by-tutor?email={email}` | Courses assigned to tutor email |

**GET /api/v1/courses — query parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `search` | string | Searches name + description |
| `level` | string | `A1` `A2` `B1` `B2` `C1` `C2` |
| `categoryId` | long | Filter by category |
| `instructorId` | long | Filter by instructor |
| `isPublished` | boolean | `true` / `false` |
| `minRating` | double | Minimum average rating |
| `freeOnly` | boolean | `true` = price == 0 |
| `sortBy` | string | `name`, `price`, `rating`, `createdAt` |
| `sortDir` | string | `asc` / `desc` |
| `page` | int | 0-based |
| `size` | int | Page size |

**CourseDTO:**
```json
{
  "courseId": 1,
  "name": "English B2",
  "description": "...",
  "level": "B2",
  "price": 150.00,
  "isPublished": true,
  "categoryId": 2,
  "instructorId": 3,
  "tutorEmail": "tutor@school.com",
  "thumbnailUrl": "http://...",
  "ratingAvg": 4.5,
  "ratingCount": 12
}
```

---

### 4.2 Modules

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/modules?courseId={id}&includeLessons=true` | Modules (optionally with nested lessons) |
| GET | `/api/v1/modules/{id}?includeLessons=true` | Module by ID |
| POST | `/api/v1/modules` | Create module |
| PUT | `/api/v1/modules/{id}` | Update module |
| DELETE | `/api/v1/modules/{id}` | Delete module |

**ModuleDTO:** `{ id, courseId, title, orderIndex, lessons[] }`

---

### 4.3 Lessons

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/lessons?moduleId={id}` | Lessons for a module (ordered by orderIndex) |
| GET | `/api/v1/lessons/{id}` | Get lesson |
| POST | `/api/v1/lessons` | Create lesson |
| PUT | `/api/v1/lessons/{id}` | Update lesson |
| DELETE | `/api/v1/lessons/{id}` | Delete lesson |

**LessonDTO:**
```json
{
  "id": 1, "moduleId": 3, "title": "Vocabulary",
  "contentType": "VIDEO",
  "contentUrl": "https://...",
  "contentText": null,
  "quizContentJson": null,
  "durationMinutes": 10,
  "orderIndex": 1
}
```
`contentType` values: `VIDEO` `PDF` `QUIZ` `TEXT`

---

### 4.4 Categories

CRUD at `/api/v1/categories`. **CategoryDTO:** `{ id, name, description, slug }`

---

### 4.5 Instructors

CRUD at `/api/v1/instructors`. **InstructorDTO:** `{ id, firstName, lastName, email, bio, avatarUrl }`

---

### 4.6 Reviews

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/reviews?courseId={id}&page=0&size=10` | Paginated reviews for course |
| POST | `/api/v1/reviews` | Create review (rating 1–5) |
| PUT | `/api/v1/reviews/{id}` | Update review |
| DELETE | `/api/v1/reviews/{id}` | Delete review |

**ReviewDTO:** `{ id, courseId, userId, rating, comment, createdAt }`

> Submitting/updating a review recalculates `course.ratingAvg` and `course.ratingCount` automatically.

---

### 4.7 File Upload

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/upload` | Upload image (JPEG/PNG/GIF/WebP, max 200 MB) |

**Response:** `{ "url": "/uploads/{uuid}.{ext}" }`

---

## 5. Enrollment Service — `/api/v1/enrollments`, `/api/v1/progress`, `/api/v1/certificates`

Internal port: **8084** | DB: **PostgreSQL** (Docker) / H2 file (local dev)

### 5.1 Enrollments

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/enrollments` | Enroll in course (wallet deduction + payment record) |
| GET | `/api/v1/enrollments` | All enrollments (Admin) |
| GET | `/api/v1/enrollments/{id}` | Get enrollment by ID |
| GET | `/api/v1/enrollments/user/{userId}/my-courses` | Student dashboard — enrolled courses + progress |
| GET | `/api/v1/enrollments/user/{userId}/history` | Full enrollment history |
| GET | `/api/v1/enrollments/user/{userId}` | Active enrollments for user |
| PATCH | `/api/v1/enrollments/{id}/cancel` | Cancel enrollment |
| PUT | `/api/v1/enrollments/{id}` | Update enrollment (Admin) |
| DELETE | `/api/v1/enrollments/{id}` | Delete enrollment (cascades progress, certificate) |

**POST /api/v1/enrollments body:**
```json
{
  "userId": 42,
  "userUuid": "uuid-from-jwt",
  "studentName": "Alice Smith",
  "courseId": 7,
  "status": "active"
}
```
> `userUuid` — UUID from JWT. When provided, the enrollment service calls auth-service to deduct `course.price` from the user's wallet, then records a `WALLET` payment via payment-service. If wallet balance < price → 500 `"Insufficient wallet balance"`.  
> **Idempotency:** send header `X-Idempotency-Key: <any-unique-string>` to prevent duplicate enrollments.

**EnrollmentDTO response:**
```json
{
  "id": 1,
  "userId": 42,
  "userUuid": "uuid",
  "studentName": "Alice Smith",
  "courseId": 7,
  "status": "active",
  "progressPercent": 0,
  "enrolledAt": "2026-04-01T10:00:00Z",
  "completedAt": null
}
```

**MyCourseDTO (`/my-courses` response items):**
```json
{
  "enrollmentId": 1,
  "courseId": 7,
  "status": "active",
  "progressPercent": 60,
  "enrolledAt": "2026-01-15T10:00:00Z",
  "completedAt": null,
  "course": { ...CourseDTO... }
}
```

---

### 5.2 Progress

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/progress/enrollments/{enrollmentId}/lessons/{lessonId}/complete` | Mark lesson complete |
| GET | `/api/v1/progress/enrollments/{enrollmentId}/lessons` | All completed lessons |
| GET | `/api/v1/progress/enrollments/{enrollmentId}/percent` | `{ "progressPercent": 75 }` |

> When `progressPercent` reaches 100%, enrollment status is automatically set to `"completed"`.

---

### 5.3 Certificates (Java PDF)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/certificates/enrollment/{enrollmentId}/download` | Download PDF certificate |

> Certificate is generated only if enrollment status is `"completed"`. Uses iText (lowagie) with branded layout.

---

## 6. Payment Service — `/api/payments`

Internal port: **8083** | DB: **MySQL**

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/payments` | Create payment record |
| GET | `/api/payments` | All payments (Admin) |
| GET | `/api/payments/{id}` | Get by ID |
| PUT | `/api/payments/{id}` | Update payment |
| DELETE | `/api/payments/{id}` | Delete payment |
| GET | `/api/payments/by-student/{studentId}` | Payments for a student |

**Payment body / response:**
```json
{
  "paymentId": 1,
  "amount": 150.00,
  "method": "WALLET",
  "status": "PENDING",
  "date": "2026-04-01T10:00:00",
  "studentId": 42,
  "courseId": 7,
  "enrollmentId": 1
}
```
`method` values: `WALLET` `CARD` `CASH` `TRANSFER`  
`status` values: `PENDING` `PAID` `FAILED`

> Payments created automatically by enrollment-service after successful wallet deduction. Manual creation available for Admin.  
> **Side-effect:** Creating a payment sends a confirmation email via JavaMailSender.

---

## 7. Reporting Service — `/api/reports`

Internal port: **8085** | DB: **MySQL**

| Method | Path | Body | Description |
|--------|------|------|-------------|
| POST | `/api/reports` | `ReportDTO` | Submit report / reclamation |
| GET | `/api/reports` | — | All reports (Admin) |
| GET | `/api/reports/{id}` | — | Get by ID |
| PUT | `/api/reports/{id}` | `ReportDTO` | Update report |
| PATCH | `/api/reports/{id}/status` | `{ "status": "RESOLVED" }` | Update status (Admin) |
| DELETE | `/api/reports/{id}` | — | Delete report |

**ReportDTO:**
```json
{
  "id": 1,
  "studentEmail": "alice@example.com",
  "subject": "Video not loading",
  "message": "Lesson 3 video fails to play.",
  "category": "TECHNICAL",
  "priority": "HIGH",
  "status": "IN_PROGRESS"
}
```
Status values: `OPEN` `IN_PROGRESS` `RESOLVED` `CLOSED`

> Reports are created with status `IN_PROGRESS` by default. `PATCH /{id}/status` updates status only when current status is not `IN_PROGRESS` (note: this is the current backend behaviour — see business logic analysis).

---

## 8. Attendance Service — `/api/attendances`

Internal port: **8087** | DB: **PostgreSQL**

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/attendances` | All attendance records |
| GET | `/api/attendances/{id}` | Get by ID |
| POST | `/api/attendances` | Create record |
| PUT | `/api/attendances/{id}` | Update record |
| DELETE | `/api/attendances/{id}` | Delete record |
| GET | `/api/attendances/student/{email}` | All records for a student |
| GET | `/api/attendances/course/{courseId}` | All records for a course |
| GET | `/api/attendances/student/{email}/course/{courseId}` | Filtered by student + course |

**Attendance body:**
```json
{
  "studentName": "Alice Smith",
  "studentEmail": "alice@example.com",
  "courseId": 7,
  "date": "2026-04-17",
  "status": "PRESENT"
}
```
`status` values: `PRESENT` `ABSENT` `LATE`

---

## 9. Schedule Service — `/api/schedules`

Internal port: **8082** | DB: **PostgreSQL**

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/schedules` | All schedules |
| GET | `/api/schedules/{id}` | Get by ID |
| POST | `/api/schedules` | Create schedule (Admin) |
| PUT | `/api/schedules/{id}` | Update schedule |
| DELETE | `/api/schedules/{id}` | Delete schedule |
| GET | `/api/schedules/course/{courseId}` | Schedules for a course |
| GET | `/api/schedules/class?name={className}` | Schedules for a class (substring match) |
| GET | `/api/schedules/weather/{dayOfWeek}/{date}` | Weather data for schedule day |

**Schedule body:**
```json
{
  "dayOfWeek": "MONDAY",
  "startTime": "09:00",
  "endTime": "11:00",
  "room": "B204",
  "courseId": 7,
  "className": "TWIN1"
}
```

**Weather response** (`GET /api/schedules/weather/MONDAY/2026-05-12`):
```json
{
  "temperature": 28.5,
  "minTemperature": 18.0,
  "rainProbability": 10,
  "weatherCode": 0,
  "condition": "☀️ Clear Sky"
}
```
> Weather data comes from Open-Meteo API (free, no key required), coordinates fixed to Tunis, Tunisia. Falls back to `22°C / Cloudy` on API failure.

---

## 10. Certificate Service (Python FastAPI) — `/api/certificates`

Internal port: **8097** | Storage: filesystem `./certificates/`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check |
| POST | `/api/certificates/generate` | Generate PDF certificate |
| GET | `/api/certificates/download/{certificate_id}` | Download PDF |
| DELETE | `/api/certificates/{certificate_id}` | Delete certificate file |

**POST /api/certificates/generate body:**
```json
{
  "studentName": "Alice Smith",
  "studentEmail": "alice@example.com",
  "examTitle": "Midterm Exam",
  "score": 17.5,
  "maxScore": 20,
  "passedAt": "2026-05-11"
}
```

**Response:**
```json
{
  "certificateId": "uuid",
  "studentName": "Alice Smith",
  "examTitle": "Midterm Exam",
  "score": 17.5,
  "downloadUrl": "/api/certificates/download/{uuid}",
  "generatedAt": "2026-05-11T14:30:00"
}
```

> Called asynchronously by assessment-service when a grade is created with `score >= maxScore * 0.5`.  
> Also callable independently via the gateway at `/api/certificates/**`.

---

## 11. Notification / Email Service — `/api/emails`

Internal port: **2001** | DB: **PostgreSQL** | Provider: **Mailjet**

| Method | Path | Body | Description |
|--------|------|------|-------------|
| POST | `/api/emails/send` | `{ "to": "...", "subject": "...", "text": "..." }` | Send email via Mailjet |

> Used internally by:
> - auth-service (email verification, password reset, parent invite)
> - assessment-service (assessment publication notifications)
> - payment-service (payment confirmation, via JavaMailSender directly)

Logs every send attempt to `email_logs` table with status `SENT` or `FAILED`.

---

## End-to-End Scenario Flows

### Flow 1: Student invites Parent
```
POST /api/auth/register  { role: "STUDENT", email: "alice@school.com" }
→ { token, userId: "alice-uuid" }

PUT /api/users/alice-uuid/parent-email
Bearer: <alice-token>
{ "parentEmail": "parent@family.com" }
→ invite email sent to parent automatically
```

### Flow 2: Parent tops up child wallet
```
POST /api/auth/register  { role: "PARENT", email: "parent@family.com" }

PUT /api/users/alice-uuid/wallet/topup
Bearer: <parent-token>
{ "amount": 300.0 }
→ { ..., walletBalance: 300.0 }
```

### Flow 3: Admin manages a class
```
POST /api/classes  { "name": "TWIN1", "level": "L3", "specialty": "TWIN" }
→ { id: 1, name: "TWIN1", ... }

PUT /api/classes/1/tutor/{tutorUuid}
PUT /api/classes/1/students/{studentUuid}
→ student's GET /api/users response now shows className: "TWIN1"
```

### Flow 4: Tutor creates and publishes an assessment
```
POST /api/assessments
{ title: "Midterm", type: "EXAM", status: "DRAFT", className: "TWIN1", startDate: "2026-05-10" }
→ { id: 12, tutorId: "...", ... }

PUT /api/assessments/12  { "status": "PUBLISHED" }
→ notifications auto-created for all students in TWIN1

POST /api/resources/upload
multipart: title="Notes", type="PDF", published=true, assessmentId=12, file=grammar.pdf
→ notification sent to TWIN1 students
```

### Flow 5: Tutor grades students
```
POST /api/grades  { assessmentId: 12, studentEmail: "alice@school.com", score: 17.5, maxScore: 20 }
→ certificate auto-requested from certificate-service (Python)

GET /api/grades/assessment/12/stats
→ { total: 24, average: 14.8, passRate: 83.33, ... }

GET /api/grades/leaderboard/assessment/12
→ ranked list with mention (EXCELLENT / GOOD / AVERAGE / FAIL)
```

### Flow 6: Student enrolls and tracks progress
```
POST /api/v1/enrollments
X-Idempotency-Key: unique-key-123
{ "userId": 42, "userUuid": "alice-uuid", "studentName": "Alice Smith", "courseId": 7 }
→ wallet 300 → 150, payment record created
→ { id: 1, status: "active", progressPercent: 0 }

POST /api/v1/progress/enrollments/1/lessons/1/complete

GET /api/v1/progress/enrollments/1/percent
→ { "progressPercent": 8 }

GET /api/v1/enrollments/user/42/my-courses

GET /api/v1/certificates/enrollment/1/download   (after 100% progress)
```

### Flow 7: Parent monitors child
```
GET /api/attendances/student/alice@school.com
GET /api/schedules/class?name=TWIN1
GET /api/users/alice-uuid/wallet

POST /api/reports
{ studentEmail: "alice@school.com", subject: "Absent", category: "ACADEMIC", priority: "MEDIUM" }
```

---

## Service Ports (direct access — development only)

| Service | Port | Eureka Name | DB |
|---------|------|-------------|-----|
| Gateway | 8080 | GATEWAY-SERVICE | — |
| Eureka | 8761 | — | — |
| Auth | 1999 | AUTH-SERVICE | PostgreSQL |
| Course | 8086 | COURSE-SERVICE | PostgreSQL (Docker) / H2 (local) |
| Enrollment | 8084 | ENROLLMENT-SERVICE | PostgreSQL (Docker) / H2-file (local) |
| Payment | 8083 | PAYMENT-SERVICE | MySQL |
| Reporting | 8085 | REPORTING-SERVICE | MySQL |
| Assessment | 8081 | ASSESSMENT-SERVICE | MySQL |
| Schedule | 8082 | SCHEDULE-SERVICE | PostgreSQL |
| Attendance | 8087 | ATTENDANCE-SERVICE | PostgreSQL |
| Resources | 8096 | RESOURCES-SERVICE | MySQL |
| Email/Notification | 2001 | NOTIFICATION-SERVICE | PostgreSQL |
| Certificate (Python) | 8097 | — (direct URL) | Filesystem |

---

## Docker — Build & Run

### Prerequisites — `.env` file

Create `Backend Services/.env`:
```env
JWT_SECRET=your-very-long-secret-key-min-32-chars
MAILJET_API_KEY=your-mailjet-api-key
MAILJET_SECRET_KEY=your-mailjet-secret-key
MAILJET_SENDER_EMAIL=noreply@yourdomain.com
MAILJET_SENDER_NAME=ESM Platform
```

> `JWT_SECRET` is shared by `auth-service`, `assessment-service`, and `resources-service`. All three must use the same value or tokens will be rejected as invalid.

### Build and start everything
```bash
docker compose up --build -d
docker compose logs -f
docker compose down
docker compose down -v   # full reset including volumes
```

### Rebuild changed services only (keep cache)
```bash
# After modifying esmauthms, assessment-service, or resources-service:
docker compose up -d --build --no-deps auth-service assessment-service resources-service

# Single service:
docker compose up -d --build --no-deps assessment-service
```

### Startup order (handled automatically by `depends_on`)
1. All databases (PostgreSQL / MySQL containers)
2. `eureka-server`
3. `email-service`, `certificate-service`
4. `auth-service`
5. All other services
6. `gateway-service` (last)

### Smoke test
```bash
curl http://localhost:8761                          # Eureka dashboard
curl http://localhost:8080/actuator/health          # Gateway health
curl http://localhost:8080/api/enums/types          # Public endpoint (no auth)
```

---

## CORS Policy

Handled at gateway level — all origins `*` allowed for all HTTP methods including `OPTIONS`. Credentials allowed. No per-service CORS config needed when going through the gateway.

---

## Error Responses

| HTTP Status | Meaning |
|-------------|---------|
| 400 | Validation error (missing required field) |
| 401 | Missing, invalid, or expired JWT |
| 403 | Authenticated but insufficient role / not the resource owner |
| 404 | Resource not found |
| 409 | Duplicate resource |
| 500 `"Insufficient wallet balance..."` | Wallet balance < course price |
| 500 `"Insufficient balance"` | Internal wallet deduction error |
