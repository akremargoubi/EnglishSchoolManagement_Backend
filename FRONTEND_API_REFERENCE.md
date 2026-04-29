# ESM Platform — Frontend API Reference

**Base URL:** `http://localhost:8080`  
**Auth header:** `Authorization: Bearer <jwt>` — required on all endpoints except login/register/enums  
**Roles:** `ADMIN` · `TUTOR` · `STUDENT` · `PARENT`  
**ADMIN is always unrestricted** on every endpoint.

---

## Auth

| Method | Path | Body |
|--------|------|------|
| POST | `/api/auth/register` | `{firstName, lastName, email, password, role}` |
| POST | `/api/auth/login` | `{email, password}` → `{token, user}` |
| POST | `/api/auth/password/reset-request` | `{email}` |
| POST | `/api/auth/password/reset-confirm` | `{token, newPassword}` |

---

## Users

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/users` | `?role=&email=&firstName=&page=&size=` |
| GET | `/api/users/{id}` | |
| POST | `/api/users` | Create user (ADMIN) |
| PUT | `/api/users/{id}` | |
| DELETE | `/api/users/{id}` | |
| GET | `/api/users/me` | Current user profile |
| PUT | `/api/users/me` | |
| POST | `/api/users/me/avatar` | `form-data: file` |
| GET | `/api/users/{id}/wallet` | `→ {userId, walletBalance}` |
| PUT | `/api/users/{id}/wallet/topup` | `{amount}` |
| PUT | `/api/users/{id}/wallet/deduct` | `{amount}` |

**User object:**
```json
{
  "id": "uuid",
  "firstName": "string",
  "lastName": "string",
  "email": "string",
  "role": "ADMIN|TUTOR|STUDENT|PARENT",
  "className": "string|null",
  "classId": "number|null",
  "walletBalance": 0.0,
  "parentEmail": "string|null",
  "avatarUrl": "string|null"
}
```

---

## Parents

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/parents` | List all parents (ADMIN) |
| POST | `/api/parents` | `{firstName, lastName, email}` (ADMIN) |
| GET | `/api/parents/me/children` | **PARENT** — returns `User[]` with walletBalance |
| GET | `/api/parents/{parentId}/children` | ADMIN |
| POST | `/api/parents/{parentId}/children/{childId}` | Link child (ADMIN) |
| DELETE | `/api/parents/{parentId}/children/{childId}` | Unlink child (ADMIN) |

---

## Classes

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/classes` | |
| POST | `/api/classes` | `{name, ...}` |
| GET | `/api/classes/{id}` | |
| PUT | `/api/classes/{id}` | |
| DELETE | `/api/classes/{id}` | |
| GET | `/api/classes/{classId}/students` | |
| PUT | `/api/classes/{classId}/students/{userId}` | Assign student |
| DELETE | `/api/classes/{classId}/students/{userId}` | Remove student |
| GET | `/api/classes/by-tutor/{tutorId}` | |
| PUT | `/api/classes/{classId}/tutor/{tutorId}` | Assign tutor |
| DELETE | `/api/classes/{classId}/tutor` | Remove tutor |

---

## Assessments

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/assessments` | ADMIN/TUTOR |
| POST | `/api/assessments` | ADMIN/TUTOR |
| GET | `/api/assessments/{id}` | |
| GET | `/api/assessments/class/{className}` | ADMIN/TUTOR/STUDENT (own class) |
| PUT | `/api/assessments/{id}` | Owner/ADMIN |
| DELETE | `/api/assessments/{id}` | Owner/ADMIN |
| GET | `/api/enums/types` | Public — assessment type values |
| GET | `/api/enums/statuses` | Public — status values |

**Assessment body:** `{title, courseName, type, status, className, startDate, endDate, duration}`  
`startDate` / `endDate` format: `"yyyy-MM-dd"`

---

## Grades

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/grades` | |
| POST | `/api/grades` | `{assessmentId, studentEmail, score, maxScore}` |
| GET | `/api/grades/{id}` | |
| PUT | `/api/grades/{id}` | |
| DELETE | `/api/grades/{id}` | |
| GET | `/api/grades/assessment/{assessmentId}` | |
| GET | `/api/grades/student/{email}` | |
| GET | `/api/grades/assessment/{assessmentId}/stats` | |
| GET | `/api/grades/leaderboard` | |
| GET | `/api/grades/leaderboard/assessment/{assessmentId}` | |

---

## Planning

| Method | Path | Params |
|--------|------|--------|
| GET | `/api/planning/calendar` | `?year=2026&month=4` |
| GET | `/api/planning/upcoming` | |
| GET | `/api/planning/ongoing` | |
| GET | `/api/planning/range` | `?start=yyyy-MM-dd&end=yyyy-MM-dd` |

---

## Notifications

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/notifications` | Current user's |
| GET | `/api/notifications/unread` | |
| GET | `/api/notifications/count` | `→ number` |
| PUT | `/api/notifications/{id}/read` | |
| PUT | `/api/notifications/read-all` | |
| DELETE | `/api/notifications/{id}` | |

---

## Schedules

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/schedules` | |
| POST | `/api/schedules` | |
| GET | `/api/schedules/{id}` | |
| PUT | `/api/schedules/{id}` | |
| DELETE | `/api/schedules/{id}` | |
| GET | `/api/schedules/class` | `?name=ClassName` |
| GET | `/api/schedules/course/{courseId}` | |

---

## Payments

| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/payments` | `{amount, method, studentId, courseId, enrollmentId, studentEmail}` |
| GET | `/api/payments` | |
| GET | `/api/payments/{id}` | |
| PUT | `/api/payments/{id}` | |
| DELETE | `/api/payments/{id}` | |
| GET | `/api/payments/by-student-email/{email}` | Lookup by student email |

`status` values: `PENDING` · `PAID` · `FAILED`

---

## Enrollments

| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/v1/enrollments` | Requires header `X-Idempotency-Key` |
| GET | `/api/v1/enrollments` | |
| GET | `/api/v1/enrollments/{id}` | |
| GET | `/api/v1/enrollments/user/{userId}/my-courses` | |
| GET | `/api/v1/enrollments/user/{userId}/history` | |
| PATCH | `/api/v1/enrollments/{id}/cancel` | |

---

## Course Progress

| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/v1/progress/enrollments/{enrollmentId}/lessons/{lessonId}/complete` | |
| GET | `/api/v1/progress/enrollments/{enrollmentId}/lessons` | |
| GET | `/api/v1/progress/enrollments/{enrollmentId}/percent` | `→ number (0-100)` |

---

## Certificates

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/certificates/enrollment/{enrollmentId}/download` | Returns PDF |

---

## Courses

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/courses` | `?search=&level=&categoryId=&page=&size=&sortBy=&sortDir=` |
| POST | `/api/v1/courses` | |
| GET | `/api/v1/courses/{id}` | |
| PUT | `/api/v1/courses/{id}` | |
| DELETE | `/api/v1/courses/{id}` | |
| PATCH | `/api/v1/courses/{id}/publish` | |
| GET | `/api/v1/courses/by-tutor` | `?email=tutor@email.com` |
| POST | `/api/v1/upload/image` | `form-data: file` → image URL |

---

## Modules

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/modules` | `?courseId=&includeLessons=true` |
| POST | `/api/v1/modules` | |
| GET | `/api/v1/modules/{id}` | |
| PUT | `/api/v1/modules/{id}` | |
| DELETE | `/api/v1/modules/{id}` | |

---

## Lessons

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/lessons` | `?moduleId=` |
| POST | `/api/v1/lessons` | types: `VIDEO` · `PDF` · `QUIZ` · `TEXT` |
| GET | `/api/v1/lessons/{id}` | |
| PUT | `/api/v1/lessons/{id}` | |
| DELETE | `/api/v1/lessons/{id}` | |

---

## Categories

| Method | Path |
|--------|------|
| GET | `/api/v1/categories` |
| POST | `/api/v1/categories` |
| GET | `/api/v1/categories/{id}` |
| PUT | `/api/v1/categories/{id}` |
| DELETE | `/api/v1/categories/{id}` |

---

## Reviews

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/reviews` | `?courseId=&page=&size=` |
| POST | `/api/v1/reviews` | `{courseId, rating (1-5), comment}` |
| GET | `/api/v1/reviews/{id}` | |
| PUT | `/api/v1/reviews/{id}` | |
| DELETE | `/api/v1/reviews/{id}` | |

---

## Attendance

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/attendances` | |
| POST | `/api/attendances` | |
| GET | `/api/attendances/{id}` | |
| PUT | `/api/attendances/{id}` | |
| DELETE | `/api/attendances/{id}` | |
| GET | `/api/attendances/student/{email}` | |
| GET | `/api/attendances/course/{courseId}` | |
| GET | `/api/attendances/student/{email}/course/{courseId}` | |

---

## Learning Resources

| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/resources/upload` | `form-data: title, type, published, assessmentId, file` |
| GET | `/api/resources/assessment/{assessmentId}` | |
| DELETE | `/api/resources/{id}` | |

---

## Reports

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/reports` | |
| POST | `/api/reports` | |
| GET | `/api/reports/{id}` | |
| PUT | `/api/reports/{id}` | |
| PATCH | `/api/reports/{id}/status` | |
| DELETE | `/api/reports/{id}` | |

---

## Email (internal use)

| Method | Path | Body |
|--------|------|------|
| POST | `/api/emails/send` | `{to, subject, text}` · optional header `X-Service-Origin` |
