# ESM Platform — Business Logic Analysis

> Code-level audit as of 2026-04-27.  
> **No code was changed** to produce this document — this is read-only analysis.

---

## Summary

| Category | Count |
|----------|-------|
| Working correctly | 14 |
| Fixed in this session | 3 |
| Known issues / bugs | 8 |
| Missing / incomplete features | 7 |

---

## 1. Working Correctly

### 1.1 Auth & JWT pipeline
JWT is issued by `esmauthms` using HMAC-SHA, claims include `sub` (userId UUID), `role`, `email`. `JwtService` uses `jjwt`. The auth-service, assessment-service, and resources-service all verify with the same secret (now correctly passed via `JWT_SECRET` env var in Docker).

### 1.2 User role management
`User.role` stored as plain uppercase string. All role checks in `CallerContext` (assessment-service) and `esmauthms` use `equalsIgnoreCase` — safe against case drift.

### 1.3 Class enrollment → immediate reflection in GET /api/users
`User.studentClass` is a direct `@ManyToOne` FK column (`class_id`). When `PUT /api/classes/{classId}/students/{userId}` is called, the user row is updated immediately. Next `GET /api/users?role=STUDENT` reflects the new `className` without any cache to invalidate.

### 1.4 Role-filtered user search
`GET /api/users?role=STUDENT` now uses `findByRoleAndDeletedAtIsNull` with `@EntityGraph(attributePaths={"studentClass"})` — single `LEFT JOIN` query, soft-deleted users excluded, no N+1.

### 1.5 className null contract
`UserServiceImpl.toDto()` only sets `className` if `user.getStudentClass() != null` and class name is non-blank. Otherwise the field serializes as JSON `null`. Jackson has no global `NON_NULL` config.

### 1.6 Assessment access control
`CallerContext.isTutor()` correctly checks `"TUTOR"`, `isStudent()` checks `"STUDENT"`. TUTOR gets filtered list (own classes via auth-service lookup). STUDENT blocked from `GET /api/assessments`.

### 1.7 Grade mention + certificate trigger
`GradeService` computes `percentage = (score/maxScore)*100`, derives mention from thresholds (≥90/≥75/≥60/<60), and calls certificate-service asynchronously when `score >= maxScore * 0.5`. Certificate call is wrapped in try/catch so a failed call does not fail the grade creation.

### 1.8 Enrollment idempotency
`EnrollmentService` computes `SHA-256(X-Idempotency-Key header)`, stores in `idempotency_keys` table. Duplicate request returns cached JSON response without re-charging the wallet. Safe for retries and network hiccups.

### 1.9 Progress tracking + auto-complete
`ProgressService.markLessonComplete` counts total lessons via `CourseFeignClient` (modules with lessons), updates `StudentProgress` table, and sets enrollment `status = "completed"` when `progressPercent == 100`. The Java certificate PDF (iText) is then available at `GET /api/v1/certificates/enrollment/{id}/download`.

### 1.10 Wallet deduction chain
Enrollment → auth-service wallet deduct → payment-service record. Each step is synchronous (Feign). If deduction fails (insufficient funds), enrollment is rejected with 500 before the payment record is created.

### 1.11 Course search with Specification pattern
`CourseSpecification` builds dynamic `WHERE` clauses via JPA `Criteria API`. All 8 filter params (search, level, categoryId, instructorId, isPublished, minRating, freeOnly, sortBy/Dir) are optional and composable.

### 1.12 Review → rating recalculation
`ReviewService` recomputes `course.ratingAvg` and `course.ratingCount` after every create/update/delete. No stale averages.

### 1.13 Notification bell (assessment-service)
`GET /api/notifications/count` returns `{ "count": N }` where N = unread notifications scoped to `callerEmail` + broadcasts. Used for UI badge.

### 1.14 Weather integration (schedule-service)
`WeatherService` calls Open-Meteo (free, no key) for Tunis coordinates. In-memory cache keyed by date. Graceful fallback to `22°C / Cloudy` on API failure. Doesn't block schedule operations.

---

## 2. Fixed in This Session

| # | What was broken | Fix applied |
|---|-----------------|-------------|
| 1 | `GET /api/users?role=STUDENT` silently ignored the `role` param — returned all roles | Added `role` to `UserSearchCriteria`, `UserController`, and `UserServiceImpl.searchUsers` |
| 2 | `assessment-service` and `resources-service` rejected all tokens with 401 "Invalid or expired JWT" in Docker | Added `JWT_SECRET: ${JWT_SECRET}` to both services in `docker-compose.yml` |
| 3 | `CallerContext.isTutor()` checked `"TEACHER"`, `isStudent()` checked `"USER"` — both always false | Corrected to `"TUTOR"` and `"STUDENT"` respectively |

---

## 3. Known Issues / Bugs

### 3.1 Reporting service: `updateStatus` logic is inverted
**File:** `ReportService.updateStatus`  
**Current code:** updates status only when `currentStatus != "IN_PROGRESS"`.  
**Problem:** New reports are created with status `"IN_PROGRESS"`. This means an admin can never update the status of a freshly created report — the one moment they most need to act on it. The condition should be the opposite: always allow status updates (or alternatively only block updates when status is already `CLOSED`/`RESOLVED`).

### 3.2 Enrollment Feign clients use hardcoded `localhost` URLs
**Files:** `CourseFeignClient`, `AuthFeignClient`, `PaymentFeignClient`  
**URLs hardcoded:** `http://localhost:8086`, `http://localhost:1999/api`, `http://localhost:8083`  
**Problem:** Inside Docker, services communicate via container name (`course-service`, `auth-service`, `payment-service`). The `application.properties` has `course-service.url=http://localhost:8086`. The Docker compose correctly overrides these via env vars (`COURSE_SERVICE_URL`, `AUTH_SERVICE_URL`, `PAYMENT_SERVICE_URL`). However, if the Feign client URL is hardcoded in the `@FeignClient` annotation rather than reading the property, Docker inter-service calls will fail. This needs verification at runtime — if enrollment → course calls fail in Docker, this is the root cause.

### 3.3 Course service data lost on restart (local dev)
**Property:** `spring.datasource.url=jdbc:h2:mem:coursedb`  
**Problem:** H2 in-memory means all courses, modules, lessons, instructors are wiped on every restart. A data loader (`CommandLineRunner`) re-seeds sample data on startup, but any changes made via API are lost. The Docker config correctly uses PostgreSQL.

### 3.4 Payment service `method` enum inconsistency
**Entity:** `PaymentEntity.method` accepts `CARD`, `CASH`, `TRANSFER`.  
**Enrollment service** creates payments with `method = "WALLET"`.  
**Problem:** `"WALLET"` is not in the payment service's accepted values. Either the payment entity needs to add `WALLET` as a valid method, or enrollment-service should use one of the existing values. Currently the payment record may be saved with an unrecognized string.

### 3.5 Parent wallet top-up has no child ownership check
**File:** `UserController.topUpWallet`  
**Problem:** Any authenticated user (including a PARENT) can call `PUT /api/users/{anyId}/wallet/topup`. There is no check that the target user is the caller's linked child. A malicious parent could top up any user's wallet.

### 3.6 `findByEmailContaining...` excludes users with null CIN or phoneNumber
**File:** `UserRepository` — text-search query  
**Problem:** The `CinContaining("")` / `PhoneNumberContaining("")` predicates translate to `WHERE cin LIKE '%'` in SQL. In PostgreSQL, `NULL LIKE '%'` evaluates to `NULL` (falsy), so users with null `cin` or null `phoneNumber` are silently excluded from general search results. Only affects the non-role search path; the role-based path uses a different query.

### 3.7 Certificate service (Python) not behind gateway auth
**Gateway route:** `Path=/api/certificates,/api/certificates/**` → `http://certificate-service:8097`  
**Problem:** The Python FastAPI service has no auth middleware. Any unauthenticated request to `POST /api/certificates/generate` via the gateway will succeed. PDF certificates can be generated for arbitrary names/emails without authentication.

### 3.8 Attendance entity primary key is named `attended`
**Entity:** `Attendance` — `@Id @GeneratedValue private Long attended`  
**Problem:** The field name `attended` is semantically a boolean (was-attended), not an ID. This is a naming confusion that doesn't break functionality but will confuse any developer querying the table or reading the entity. The existing API uses it as a numeric ID.

---

## 4. Missing / Incomplete Features

### 4.1 No per-role authorization on most services
**Affected:** course-service, enrollment-service, payment-service, reporting-service, attendance-service, schedule-service  
All `SecurityConfig` instances use `anyRequest().permitAll()`. Any authenticated user (or even unauthenticated for some) can call any endpoint. For example:
- Any student can create/delete schedules
- Any user can view all payments
- Any user can update/delete any attendance record

**What to do:** Add `callerRole` extraction from JWT headers forwarded by the gateway and enforce role checks in controllers or a filter, similar to the pattern used in assessment-service.

### 4.2 Assessment service calls auth-service with plain `RestTemplate` (no auth header)
**File:** `AuthServiceClient` — uses `new RestTemplate()` with no headers.  
`GET /api/users/{userId}` and `GET /api/classes/by-tutor/{tutorId}` are called without a JWT token.  
**Problem:** If auth-service endpoints are ever secured, these calls will start failing with 401. Currently works because auth-service allows all requests.

### 4.3 No grade-to-student validation
**File:** `GradeService.create`  
The grade is created with `studentEmail` and `studentName` as free-text strings. There is no call to auth-service to verify the student exists or is enrolled in the assessment's class. A tutor can grade a non-existent email.

### 4.4 No assessment notification for STUDENT role (only class-level broadcast)
When an assessment is published, the notification is sent to all students in `className`. However, there is no mechanism to notify students who are not enrolled in a class (className is null) or to send personalized notifications when a grade is posted for a specific student.

### 4.5 Enrollment service certificate conflicts with Python certificate service
The enrollment-service has its own `CertificateService` using iText (Java, at `/api/v1/certificates/`) AND there is a separate Python FastAPI certificate service (at `/api/certificates/`). Both generate PDFs but for different flows:
- Java cert (enrollment): triggered by 100% course progress — branded English School course completion
- Python cert (assessment): triggered by passing an exam grade — branded exam certificate

These are intentionally different but documentation must be clear which frontend flow uses which.

### 4.6 Resources service has no `GET /api/resources/{id}` or `GET /api/resources` (list all)
**File:** `LearningResourceController` only exposes:
- `POST /upload`
- `GET /assessment/{assessmentId}`
- `DELETE /{id}`

There is no endpoint to fetch a single resource by ID or list all resources globally. If the frontend needs to display a resource detail page by URL or link, this is missing.

### 4.7 Schedule-service has no recurrence model
Each `Schedule` is a single `dayOfWeek` + `startTime` + `endTime` record. There is no date range, no semester start/end, no exception dates (holidays). The calendar view requires the frontend to expand `MONDAY` into actual dates manually. A proper schedule model would include `validFrom` / `validUntil` dates.

---

## 5. Architecture Observations

### Polyglot persistence (by design)
| DB | Services |
|----|----------|
| PostgreSQL | auth, schedule, attendance, enrollment (Docker), course (Docker), email/notification |
| MySQL | assessment, resources, payment, reporting |
| H2 in-memory | course (local), enrollment (local) |
| Filesystem | certificate (Python) |

This is functional but means 3 different DB admin tools and different SQL dialects. Acceptable for a student project.

### Two certificate services
Two independent PDF generators for two different use cases (course completion vs. exam pass). Both work. Gateway routes are distinct. The naming collision (`/api/v1/certificates` vs `/api/certificates`) is easy to confuse.

### Feign vs RestTemplate
- enrollment-service uses **Feign** (declarative, cleaner) for inter-service calls
- assessment-service uses **RestTemplate** (manual) for auth-service calls
- payment-service uses **JavaMailSender** directly (not the central email-service)

No standardization. Not a bug, but creates inconsistent error-handling patterns.

### Eureka registration
- course-service and enrollment-service have `register-with-eureka=false` in local `application.properties`
- Docker overrides set `EUREKA_CLIENT_REGISTER_WITH_EUREKA=true`
- Works correctly in Docker; services won't appear in local Eureka dashboard

### S3 / MinIO for avatars
Auth-service uploads avatars to `https://esms3.dominnovate.com` (production MinIO). This is a real external endpoint — avatar uploads from local dev will go to production storage. Consider a local MinIO container for development isolation.

---

## 6. Priority Fix List

Ordered by impact on current frontend integration:

| Priority | Issue | Effort |
|----------|-------|--------|
| 🔴 High | 3.1 — Reporting `updateStatus` logic inverted — admin can't update new reports | 1 line |
| 🔴 High | 3.2 — Verify enrollment Feign clients resolve correctly in Docker | Test only |
| 🔴 High | 3.4 — `WALLET` not a valid payment `method` in payment-service | Add enum value |
| 🟡 Medium | 3.5 — Any user can top up any wallet (no ownership check) | ~15 lines |
| 🟡 Medium | 4.1 — No role enforcement on course/enrollment/schedule/attendance/reporting | Per-service filter |
| 🟡 Medium | 4.3 — No student existence check when creating a grade | AuthServiceClient call |
| 🟡 Medium | 4.6 — Resources service missing `GET /api/resources/{id}` | 1 controller method |
| 🟢 Low | 3.6 — null CIN/phone excludes users from text search | JPQL fix |
| 🟢 Low | 3.7 — Certificate service unprotected via gateway | FastAPI auth middleware |
| 🟢 Low | 3.8 — Attendance PK named `attended` | Rename (migration needed) |
| 🟢 Low | 4.7 — Schedule has no date range / recurrence | Data model change |
