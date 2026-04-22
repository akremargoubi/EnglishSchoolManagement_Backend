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
- `userId` — UUID (string) of the user
- `role` — one of: `ADMIN`, `STUDENT`, `TUTOR`, `PARENT`
- `status` — user account status

---

## 1. Auth Service — `/api/auth` & `/api/users`

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
Roles: `ADMIN`, `STUDENT`, `TUTOR`, `PARENT`

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

| Method | Path | Body / Params | Description |
|--------|------|---------------|-------------|
| GET | `/api/users/me` | Bearer token | Get current user profile |
| PUT | `/api/users/me` | `UserSelfUpdateRequest` | Update own profile |
| POST | `/api/users/me/avatar` | multipart `file` | Upload avatar |
| GET | `/api/users` | `?email=&firstName=&lastName=&page=&size=` | Search users (paginated) |
| GET | `/api/users/{id}` | — | Get user by UUID |
| POST | `/api/users` | `UserCreateRequest` | Create user (Admin) |
| PUT | `/api/users/{id}` | `UserUpdateRequest` | Update user (Admin) |
| DELETE | `/api/users/{id}` | — | Delete user (Admin) |

**Wallet & Parent endpoints (NEW):**

| Method | Path | Body | Description |
|--------|------|------|-------------|
| GET | `/api/users/{id}/wallet` | — | Get wallet balance → `{ userId, walletBalance }` |
| PUT | `/api/users/{id}/wallet/topup` | `{ "amount": 100.0 }` | Add funds to user wallet (Parent tops up child) |
| PUT | `/api/users/{id}/wallet/deduct` | `{ "amount": 50.0 }` | Deduct funds (called internally by enrollment) |
| PUT | `/api/users/{id}/parent-email` | `{ "parentEmail": "parent@email.com" }` | Student sets parent email — triggers invite email to parent |

**UserResponseDto fields:**
```json
{
  "id": "uuid",
  "firstName": "Alice",
  "lastName": "Smith",
  "email": "alice@example.com",
  "role": "STUDENT",
  "status": "ACTIVE",
  "cin": "12345678",
  "phoneNumber": "+21612345678",
  "avatarUrl": "http://...",
  "walletBalance": 250.0,
  "parentEmail": "parent@email.com"
}
```

---

### 1.3 Student Classes

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/classes` | Get all classes |
| GET | `/api/classes/{id}` | Get class by ID |
| POST | `/api/classes` | Create class |
| PUT | `/api/classes/{id}` | Update class |
| DELETE | `/api/classes/{id}` | Delete class |

---

## 2. Course Service — `/api/v1/courses`

### 2.1 Courses

| Method | Path | Body / Params | Description |
|--------|------|---------------|-------------|
| GET | `/api/v1/courses` | `?search=&level=&categoryId=&instructorId=&isPublished=&minRating=&freeOnly=&sortBy=&sortDir=&page=&size=` | List / search courses |
| GET | `/api/v1/courses/{id}` | — | Get course by ID |
| POST | `/api/v1/courses` | `CourseDTO` | Create course (Admin/Tutor) |
| PUT | `/api/v1/courses/{id}` | `CourseDTO` | Update course |
| PATCH | `/api/v1/courses/{id}/publish` | — | Publish course |
| DELETE | `/api/v1/courses/{id}` | — | Delete course |
| PUT | `/api/v1/courses/{id}/assign-tutor` | `{ "tutorEmail": "tutor@school.com" }` | Assign tutor to course (Admin) **(NEW)** |
| GET | `/api/v1/courses/by-tutor?email=X` | — | Get courses assigned to tutor email **(NEW)** |

**CourseDTO key fields:**
```json
{
  "courseId": 1,
  "title": "English B2",
  "description": "...",
  "level": "B2",
  "price": 150.00,
  "isPublished": true,
  "categoryId": 2,
  "instructorId": 3,
  "tutorEmail": "tutor@school.com",
  "imageUrl": "http://...",
  "averageRating": 4.5,
  "totalLessons": 12
}
```

---

### 2.2 Modules

| Method | Path | Body / Params | Description |
|--------|------|---------------|-------------|
| GET | `/api/v1/modules?courseId={id}` | `&includeLessons=true` | Get modules for a course |
| GET | `/api/v1/modules/{id}` | `?includeLessons=true` | Get module by ID |
| POST | `/api/v1/modules` | `ModuleDTO` | Create module |
| PUT | `/api/v1/modules/{id}` | `ModuleDTO` | Update module |
| DELETE | `/api/v1/modules/{id}` | — | Delete module |

**ModuleDTO:**
```json
{ "id": 1, "courseId": 5, "title": "Unit 1 - Introduction", "orderIndex": 1 }
```

---

### 2.3 Lessons

| Method | Path | Body | Description |
|--------|------|------|-------------|
| GET | `/api/v1/lessons?moduleId={id}` | — | Get lessons for a module |
| GET | `/api/v1/lessons/{id}` | — | Get lesson by ID |
| POST | `/api/v1/lessons` | `LessonDTO` | Create lesson (Tutor adds content) |
| PUT | `/api/v1/lessons/{id}` | `LessonDTO` | Update lesson |
| DELETE | `/api/v1/lessons/{id}` | — | Delete lesson |

**LessonDTO:**
```json
{
  "id": 1,
  "moduleId": 3,
  "title": "Vocabulary - Unit 1",
  "contentType": "VIDEO",
  "contentUrl": "https://...",
  "duration": 600,
  "orderIndex": 1
}
```
`contentType` values: `VIDEO`, `PDF`, `QUIZ`, `TEXT`

---

### 2.4 Categories

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/categories` | List all categories |
| GET | `/api/v1/categories/{id}` | Get category |
| POST | `/api/v1/categories` | Create category |
| PUT | `/api/v1/categories/{id}` | Update category |
| DELETE | `/api/v1/categories/{id}` | Delete category |

---

### 2.5 Instructors

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/instructors` | List all instructors |
| GET | `/api/v1/instructors/{id}` | Get instructor |
| POST | `/api/v1/instructors` | Create instructor profile |
| PUT | `/api/v1/instructors/{id}` | Update instructor |
| DELETE | `/api/v1/instructors/{id}` | Delete instructor |

---

### 2.6 Reviews

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/reviews?courseId={id}` | Get reviews for course |
| POST | `/api/v1/reviews` | Submit review |
| DELETE | `/api/v1/reviews/{id}` | Delete review |

---

### 2.7 File Upload

| Method | Path | Body | Description |
|--------|------|------|-------------|
| POST | `/api/v1/upload` | multipart `file` | Upload file (video/PDF), returns URL |

---

## 3. Enrollment Service — `/api/v1/enrollments`, `/api/v1/progress`, `/api/v1/certificates`

### 3.1 Enrollments

| Method | Path | Body / Header | Description |
|--------|------|---------------|-------------|
| POST | `/api/v1/enrollments` | `EnrollmentDTO` + optional `X-Idempotency-Key` header | Enroll in course — deducts wallet if `userUuid` provided |
| GET | `/api/v1/enrollments` | — | All enrollments (Admin) |
| GET | `/api/v1/enrollments/{id}` | — | Get enrollment by ID |
| GET | `/api/v1/enrollments/user/{userId}/my-courses` | — | Student dashboard — enrolled courses + progress |
| GET | `/api/v1/enrollments/user/{userId}/history` | — | Full enrollment history |
| GET | `/api/v1/enrollments/user/{userId}` | — | Active enrollments for user |
| PATCH | `/api/v1/enrollments/{id}/cancel` | — | Cancel enrollment |
| PUT | `/api/v1/enrollments/{id}` | `EnrollmentDTO` | Update enrollment (Admin) |
| DELETE | `/api/v1/enrollments/{id}` | — | Delete enrollment |

**EnrollmentDTO (POST body):**
```json
{
  "userId": 42,
  "userUuid": "uuid-from-jwt",
  "studentName": "Alice Smith",
  "courseId": 7,
  "status": "active"
}
```
> `userUuid` is the UUID string from the JWT. When provided, the enrollment service calls the auth service to deduct `course.price` from the user's wallet, then records a WALLET payment. If omitted or wallet has insufficient funds, enrollment is rejected with 500 + `"Insufficient wallet balance"` message.

**MyCourseDTO (response of `/my-courses`):**
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

### 3.2 Progress

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/progress/enrollments/{enrollmentId}/lessons/{lessonId}/complete` | Mark lesson as complete |
| GET | `/api/v1/progress/enrollments/{enrollmentId}/lessons` | Get all completed lessons for enrollment |
| GET | `/api/v1/progress/enrollments/{enrollmentId}/percent` | Get completion % → `{ "progressPercent": 75 }` |

---

### 3.3 Certificates

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/certificates/enrollment/{enrollmentId}/download` | Download PDF certificate |

---

## 4. Payment Service — `/api/payments`

| Method | Path | Body | Description |
|--------|------|------|-------------|
| POST | `/api/payments` | `PaymentBody` | Create payment record |
| GET | `/api/payments` | — | Get all payments (Admin) |
| GET | `/api/payments/{id}` | — | Get payment by ID |
| PUT | `/api/payments/{id}` | `PaymentBody` | Update payment |
| DELETE | `/api/payments/{id}` | — | Delete payment |
| GET | `/api/payments/by-student/{studentId}` | — | Get payments for a student |

Payments are created automatically by the enrollment service when a wallet deduction succeeds. Manual creation is available for Admin.

**Payment fields:** `amount`, `method` (`WALLET`/`CARD`/`CASH`), `studentId`, `courseId`, `enrollmentId`, `status` (`PAID`/`PENDING`/`REFUNDED`)

---

## 5. Reporting Service — `/api/reports`

| Method | Path | Body | Description |
|--------|------|------|-------------|
| POST | `/api/reports` | `ReportDTO` | Submit report / reclamation |
| GET | `/api/reports` | — | Get all reports (Admin) |
| GET | `/api/reports/{id}` | — | Get report by ID |
| PUT | `/api/reports/{id}` | `ReportDTO` | Update report |
| PATCH | `/api/reports/{id}/status` | `{ "status": "RESOLVED" }` | Update status (Admin) |
| DELETE | `/api/reports/{id}` | — | Delete report |

**ReportDTO fields:**
```json
{
  "id": 1,
  "studentEmail": "alice@example.com",
  "subject": "Issue with lesson video",
  "message": "The video does not load.",
  "category": "TECHNICAL",
  "priority": "HIGH",
  "status": "OPEN"
}
```
Status values: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`

---

## 6. Attendance Service — `/api/attendances`

| Method | Path | Body | Description |
|--------|------|------|-------------|
| GET | `/api/attendances` | — | Get all attendance records |
| GET | `/api/attendances/{id}` | — | Get by ID |
| POST | `/api/attendances` | `Attendance` | Create attendance record |
| PUT | `/api/attendances/{id}` | `Attendance` | Update attendance |
| DELETE | `/api/attendances/{id}` | — | Delete |
| GET | `/api/attendances/student/{email}` | — | All records for a student (Parent view) **(NEW)** |
| GET | `/api/attendances/course/{courseId}` | — | All records for a course **(NEW)** |
| GET | `/api/attendances/student/{email}/course/{courseId}` | — | Filtered by student + course **(NEW)** |

**Attendance fields:**
```json
{
  "id": 1,
  "studentName": "Alice Smith",
  "studentEmail": "alice@example.com",
  "courseId": 7,
  "date": "2026-04-17",
  "status": "PRESENT"
}
```
> `studentEmail` and `courseId` are new optional fields. Existing records without them are unaffected.

---

## 7. Schedule Service — `/api/schedules`

| Method | Path | Params | Description |
|--------|------|--------|-------------|
| GET | `/api/schedules` | — | Get all schedules |
| GET | `/api/schedules/{id}` | — | Get by ID |
| POST | `/api/schedules` | `Schedule` | Create schedule (Admin) |
| PUT | `/api/schedules/{id}` | `Schedule` | Update schedule |
| DELETE | `/api/schedules/{id}` | — | Delete schedule |
| GET | `/api/schedules/weather/{dayOfWeek}/{date}` | e.g. `MONDAY/2026-04-17` | Weather for schedule day |
| GET | `/api/schedules/course/{courseId}` | — | Schedules for a course **(NEW)** |
| GET | `/api/schedules/class?name=X` | `name` substring match | Schedules for a class name **(NEW)** |

**Schedule fields:**
```json
{
  "id": 1,
  "dayOfWeek": "MONDAY",
  "startTime": "09:00",
  "endTime": "11:00",
  "room": "B204",
  "courseId": 7,
  "className": "TWIN1"
}
```
> `courseId` and `className` are new optional fields for filtering.

---

## 8. Assessment Service — `/api/assessments`, `/api/grades`, `/api/enums`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/assessments` | List all assessments |
| GET | `/api/assessments/{id}` | Get assessment |
| POST | `/api/assessments` | Create assessment |
| PUT | `/api/assessments/{id}` | Update assessment |
| DELETE | `/api/assessments/{id}` | Delete assessment |
| GET | `/api/grades` | List grades |
| GET | `/api/grades/{id}` | Get grade |
| POST | `/api/grades` | Submit grade |
| PUT | `/api/grades/{id}` | Update grade |
| DELETE | `/api/grades/{id}` | Delete grade |
| GET | `/api/enums` | Get enum values used by assessment forms |

---

## 9. Resources Service — `/api/resources`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/resources` | List resources |
| GET | `/api/resources/{id}` | Get resource |
| POST | `/api/resources` | Upload/create resource |
| PUT | `/api/resources/{id}` | Update resource |
| DELETE | `/api/resources/{id}` | Delete resource |

---

## 10. Certificate Service (Python FastAPI) — `/api/certificates`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/certificates` | Generate certificate PDF |
| GET | `/api/certificates/{id}` | Get certificate |
| GET | `/api/certificates/health` | Health check |

---

## 11. Notification / Email Service — `/api/emails`

| Method | Path | Body | Description |
|--------|------|------|-------------|
| POST | `/api/emails/send` | `{ to, subject, body }` | Send email via Mailjet |

> Used internally by auth-service (email verification, password reset, parent invite).

---

## End-to-End Scenario Flows

### Flow 1: Student invites Parent

```
1. Student registers:
   POST /api/auth/register  { role: "STUDENT", email: "alice@school.com", ... }
   → response: { token, userId: "alice-uuid" }

2. Student sets parent email:
   PUT /api/users/alice-uuid/parent-email
   Authorization: Bearer <alice-token>
   Body: { "parentEmail": "parent@family.com" }
   → Parent receives invite email automatically
```

### Flow 2: Parent tops up child wallet

```
1. Parent registers (using the invited email):
   POST /api/auth/register  { role: "PARENT", email: "parent@family.com", ... }

2. Parent tops up child wallet:
   PUT /api/users/alice-uuid/wallet/topup
   Authorization: Bearer <parent-token>
   Body: { "amount": 300.0 }
   → Response: { ..., walletBalance: 300.0 }

3. Check wallet:
   GET /api/users/alice-uuid/wallet
   → { "userId": "alice-uuid", "walletBalance": 300.0 }
```

### Flow 3: Admin creates course and assigns tutor

```
1. Admin creates course:
   POST /api/v1/courses
   Body: { "title": "English B2", "level": "B2", "price": 150.00, "categoryId": 1 }
   → { courseId: 7, ... }

2. Admin assigns tutor:
   PUT /api/v1/courses/7/assign-tutor
   Body: { "tutorEmail": "tutor@school.com" }

3. Admin creates schedule:
   POST /api/schedules
   Body: { "dayOfWeek": "MONDAY", "startTime": "09:00", "endTime": "11:00", "room": "B204", "courseId": 7, "className": "TWIN1" }
```

### Flow 4: Tutor adds course content

```
1. Tutor views assigned courses:
   GET /api/v1/courses/by-tutor?email=tutor@school.com

2. Tutor creates module:
   POST /api/v1/modules
   Body: { "courseId": 7, "title": "Unit 1", "orderIndex": 1 }
   → { id: 3, ... }

3. Tutor creates lesson:
   POST /api/v1/lessons
   Body: { "moduleId": 3, "title": "Vocabulary", "contentType": "VIDEO", "contentUrl": "https://...", "orderIndex": 1 }
```

### Flow 5: Student enrolls and tracks progress

```
1. Student enrolls (wallet deduction):
   POST /api/v1/enrollments
   X-Idempotency-Key: unique-key-123
   Body: {
     "userId": 42,
     "userUuid": "alice-uuid",
     "studentName": "Alice Smith",
     "courseId": 7
   }
   → wallet: 300 → 150, payment record created, enrollment returned
   → { id: 1, status: "active", progressPercent: 0, ... }

2. Student marks lesson complete:
   POST /api/v1/progress/enrollments/1/lessons/1/complete

3. Check progress:
   GET /api/v1/progress/enrollments/1/percent
   → { "progressPercent": 8 }

4. Student views my courses:
   GET /api/v1/enrollments/user/42/my-courses
   → [ { enrollmentId, courseId, progressPercent, course: {...} } ]

5. Download certificate (when complete):
   GET /api/v1/certificates/enrollment/1/download
```

### Flow 6: Parent monitors child

```
1. View child attendance:
   GET /api/attendances/student/alice@school.com

2. View class schedule:
   GET /api/schedules/class?name=TWIN1

3. Submit report:
   POST /api/reports
   Body: { "studentEmail": "alice@school.com", "subject": "Absent lesson", "message": "...", "category": "ACADEMIC", "priority": "MEDIUM" }

4. Admin resolves report:
   PATCH /api/reports/1/status
   Body: { "status": "RESOLVED" }
```

---

## Service Ports (direct access, for development only)

| Service | Port | Eureka Name |
|---------|------|-------------|
| Gateway | 8080 | GATEWAY-SERVICE |
| Eureka | 8761 | — |
| Auth | 1999 | AUTH-SERVICE |
| Course | 8086 | COURSE-SERVICE |
| Enrollment | 8084 | ENROLLMENT-SERVICE |
| Payment | 8083 | PAYMENT-SERVICE |
| Reporting | 8085 | REPORTING-SERVICE |
| Assessment | 8081 | ASSESSMENT-SERVICE |
| Schedule | 8082 | SCHEDULE-SERVICE |
| Attendance | 8087 | ATTENDANCE-SERVICE |
| Resources | 8096 | RESOURCES-SERVICE |
| Email/Notification | 2001 | NOTIFICATION-SERVICE |
| Certificate (Python) | 8097 | — (direct URL) |

---

## Docker — Build & Run

### Prerequisites

Create a `.env` file in `Backend Services/` directory:
```env
JWT_SECRET=your-very-long-secret-key-min-32-chars
MAILJET_API_KEY=your-mailjet-api-key
MAILJET_SECRET_KEY=your-mailjet-secret-key
MAILJET_SENDER_EMAIL=noreply@yourdomain.com
MAILJET_SENDER_NAME=ESM Platform
```

### Build and start all services

```bash
# From: d:\PI 4 SAE INTEGRATION\Backend Services\

# Build all images and start everything
docker compose up --build -d

# Watch logs
docker compose logs -f

# Watch specific service
docker compose logs -f enrollment-service

# Stop everything
docker compose down

# Stop and delete all data volumes (full reset)
docker compose down -v
```

### Start order (handled automatically by depends_on)

1. Databases (postgres/mysql containers)
2. `eureka-server`
3. `email-service` (messaging)
4. `certificate-service` (Python FastAPI)
5. `auth-service`
6. All other services
7. `gateway-service` (last)

### Rebuild a single service after code change

```bash
docker compose up --build -d auth-service
# or
docker compose build course-service && docker compose up -d course-service
```

### Build all Spring Boot JARs locally (before docker build)

```bash
# Run from each service directory that has a pom.xml
# Example — build all at once using a loop:
for dir in esmauthms enrollment-service course-service payment_Microsevices reporting-service \
            assessment-service resources-service attendance-service schedule-service \
            messaging-service gateway-service discovery-service; do
  echo "=== Building $dir ==="
  (cd "$dir" && ./mvnw clean package -DskipTests -q)
done
```

On Windows (PowerShell):
```powershell
$services = @("esmauthms","enrollment-service","course-service","payment_Microsevices",
              "reporting-service","assessment-service","resources-service",
              "attendance-service","schedule-service","messaging-service",
              "gateway-service","discovery-service")
foreach ($svc in $services) {
  Write-Host "=== Building $svc ==="
  Push-Location $svc
  & .\mvnw.cmd clean package -DskipTests -q
  Pop-Location
}
```

### Verify all services are up

```bash
# Eureka dashboard
open http://localhost:8761

# Gateway health
curl http://localhost:8080/actuator/health

# Quick smoke test
curl http://localhost:8080/api/v1/courses
curl http://localhost:8080/api/schedules
```

---

## CORS Policy

All origins (`*`) are permitted at the gateway level for all HTTP methods including OPTIONS. Credentials are allowed. No per-service CORS configuration is needed when going through the gateway.

---

## Error Responses

| HTTP Status | Meaning |
|-------------|---------|
| 400 | Validation error (missing required field) |
| 401 | Missing or invalid JWT |
| 404 | Resource not found |
| 409 | Duplicate resource |
| 500 `"Insufficient wallet balance..."` | Wallet has less than course price |
| 500 `"Insufficient balance"` | Internal wallet deduction error |
