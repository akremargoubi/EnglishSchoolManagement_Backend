# 🎓 Fluencity — English School Management Platform

> **PIDEV 2025-2026** | Esprit School of Engineering — 4SAE4  
> Supervised by **Nadine Mili**

A full-stack microservices-based platform for managing an English language school, built with Spring Boot, Angular, Docker, and MySQL/PostgreSQL.

---

## 👥 Team

| Member | Role | Microservices |
|--------|------|---------------|
| **Firas Tourki** | Full Stack | Assessment Service · Learning Resources Service |
| **Akrem Argoubi** | Full Stack | Auth Service · User Management |
| **Youssef** | Full Stack | Course Service · Enrollment Service |
| **Rayen** | Full Stack | Attendance Service · Schedule Service |
| **Kamel** | Full Stack | Payment Service · Reporting Service |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Angular Frontend                      │
│           localhost:4200                                 │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│              Spring Cloud Gateway                        │
│                  localhost:8080                          │
└──┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬───┘
   │    │    │    │    │    │    │    │    │    │    │
  Auth  Asmt Rsrc Crs  Enrl Att  Sch  Pay  Rpt  Cert Msg
 1999  8081 8096 8086 8084 8087 8082 8083 8085 8097 2001
```

### Microservices

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| `auth-service` | 1999 | PostgreSQL | Authentication, JWT, User Management |
| `assessment-service` | 8081 | MySQL | Exams, Quizzes, Grades, Leaderboard |
| `resources-service` | 8096 | MySQL | Learning Materials, Files |
| `course-service` | 8086 | PostgreSQL | Course Catalog, Categories |
| `enrollment-service` | 8084 | PostgreSQL | Student Enrollments |
| `attendance-service` | 8087 | PostgreSQL | Attendance Tracking |
| `schedule-service` | 8082 | PostgreSQL | Class Timetables |
| `payment-service` | 8083 | MySQL | Tuition & Fees |
| `reporting-service` | 8085 | MySQL | Reclamations & Reports |
| `certificate-service` | 8097 | — | PDF Certificate Generation (Python/FastAPI) |
| `messaging-service` | 2001 | PostgreSQL | Email Notifications |
| `gateway-service` | 8080 | — | Spring Cloud Gateway |
| `eureka-server` | 8761 | — | Service Discovery |

---

## 🚀 Getting Started

### Prerequisites

- Docker Desktop
- Node.js 18+
- Angular CLI 17+
- Java 17+
- Maven 3.9+

### Backend

```bash
# Clone the repository
git clone https://github.com/akremargoubi/EnglishSchoolManagement_Backend.git
cd EnglishSchoolManagement_Backend

# Start all services
docker compose up --build

# Check Eureka dashboard
open http://localhost:8761
```

### Frontend

```bash
# Clone the repository
git clone <frontend-repo-url>
cd esm-front

# Install dependencies
npm install

# Start development server
ng serve

# Open browser
open http://localhost:4200
```

---

## 🔐 Authentication & RBAC

The platform uses **JWT-based authentication** with role-based access control.

| Role | Dashboard | Access |
|------|-----------|--------|
| `ADMIN` | `/backoffice/dashboard` | Full platform management |
| `TUTOR` | `/backoffice/dashboard` | Teaching tools |
| `STUDENT` | `/student/home` | Learning portal |
| `PARENT` | `/parent/dashboard` | Child monitoring |

### Demo Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@test.com` | `admin123` |
| Student | `student@test.com` | `student123` |
| Parent | `parent@test.com` | `parent123` |

---

## 📱 Portal Overview

### Admin Portal (`/backoffice`)
- 📊 School Overview Dashboard (students, classes, courses, assessments, resources)
- 📝 Assessment Planning & Management
- 🏆 Grades & Leaderboard
- 📚 Learning Resources
- ✅ Attendance Tracking
- 🗓️ Class Scheduling
- 📖 Course & Enrollment Management
- 💰 Payment Management
- 📋 Reclamations & Reports
- 👥 User & Class Management

### Student Portal (`/student`)
- 🏠 Dashboard with upcoming alerts (48h)
- 📚 My Courses
- ✅ Attendance Records
- 🏆 Grades & Performance
- 📝 Published Assessments
- 🥇 Global Leaderboard
- 🗓️ Class Schedule
- 📜 Certificate Generation

### Parent Portal (`/parent`)
- 🏠 Overview Dashboard
- 🏆 Child Grades Monitoring
- 📅 Planning (upcoming exams + schedules)
- 💳 Payment History

---

## 🛠️ Tech Stack

### Backend
- **Spring Boot 3.2** — Microservices framework
- **Spring Cloud Gateway** — API Gateway & routing
- **Spring Cloud Eureka** — Service discovery
- **Spring Security + JWT** — Authentication & authorization
- **Spring Data JPA + Hibernate** — ORM
- **Spring Mail** — Email notifications
- **FastAPI (Python)** — Certificate generation service
- **MySQL 8.4** — Relational database (assessment, payment, reporting, resources)
- **PostgreSQL 16** — Relational database (auth, course, enrollment, attendance, schedule)
- **Docker & Docker Compose** — Containerization

### Frontend
- **Angular 17** — SPA framework
- **TypeScript** — Type-safe JavaScript
- **Tailwind CSS** — Utility-first styling
- **RxJS** — Reactive programming
- **Chart.js / Recharts** — Data visualization
- **jsPDF** — PDF export
- **QRCode** — QR code generation

---

## 📊 Key Features

### Firas — Assessment & Learning Resources
- ✅ Full CRUD for Assessments (EXAM, QUIZ, PROJECT)
- ✅ Grade management with automatic percentage calculation
- ✅ Global leaderboard with rankings
- ✅ PDF certificate generation via Python microservice
- ✅ Async email notifications (48h before exams)
- ✅ Learning resources linked to assessments
- ✅ Class-based assessment filtering

### Akrem — Auth & Users
- ✅ JWT authentication with role-based access
- ✅ Email verification flow
- ✅ Two-factor authentication (2FA)
- ✅ Password reset via email
- ✅ User CRUD with pagination & filters
- ✅ Class assignment for students

### Youssef — Courses & Enrollment
- ✅ Course catalog with categories & instructors
- ✅ Student enrollment management
- ✅ Course detail pages with modules & reviews
- ✅ Thumbnail normalization

### Rayen — Attendance & Schedule
- ✅ Attendance tracking (PRESENT/ABSENT/LATE)
- ✅ Analytics dashboard
- ✅ Weekly schedule management
- ✅ Weather widget integration
- ✅ Conflict detection

### Kamel — Payments & Reports
- ✅ Payment management (CASH, BANK_TRANSFER)
- ✅ PDF export with autoTable
- ✅ Reclamation management with status workflow
- ✅ QR code generation per report
- ✅ Auto-refresh every 10 seconds

---

## 🔧 Environment Configuration

### Backend `.env`
```env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
MYSQL_ROOT_PASSWORD=root
JWT_SECRET=your-secret-key
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### Frontend `environment.ts`
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  enrollmentApiUrl: 'http://localhost:8080',
  courseApiUrl: 'http://localhost:8080'
};
```

---

## 📁 Project Structure

```
EnglishSchoolManagement/
├── Backend/
│   ├── esmauthms/              # Auth + User management
│   ├── assessment-service/     # Assessments + Grades
│   ├── resources-service/      # Learning resources
│   ├── course-service/         # Courses
│   ├── Enrollment-service/     # Enrollments
│   ├── attendance-service/     # Attendance
│   ├── schedule-service/       # Schedules
│   ├── payment_Microsevices/   # Payments
│   ├── reporting-service/      # Reports
│   ├── certificate-service/    # PDF certificates (Python)
│   ├── messaging-service/      # Email notifications
│   ├── gateway-service/        # API Gateway
│   ├── discovery-service/      # Eureka
│   └── docker-compose.yml
│
└── Frontend/
    └── esm-front/
        ├── src/app/
        │   ├── layout/
        │   │   ├── admin-layout/
        │   │   ├── student-layout/
        │   │   └── parent-layout/
        │   ├── pages/
        │   │   ├── backoffice/     # Firas
        │   │   ├── dashboard/      # Firas
        │   │   ├── grades/         # Firas
        │   │   ├── leaderboard/    # Firas
        │   │   ├── resources/      # Firas
        │   │   ├── planning/       # Firas
        │   │   ├── attendance/     # Rayen
        │   │   ├── schedule/       # Rayen
        │   │   ├── courses/        # Youssef
        │   │   ├── enrollment/     # Youssef
        │   │   ├── payments/       # Kamel
        │   │   ├── reports/        # Kamel
        │   │   ├── users/          # Akrem
        │   │   ├── student/        # Student portal
        │   │   └── parent/         # Parent portal
        │   └── services/
        └── package.json
```

---

## 🧪 API Endpoints

### Auth Service (`/api/auth`)
```
POST /api/auth/register     — Register new user
POST /api/auth/login        — Login → JWT token
POST /api/auth/email/verify — Verify email
POST /api/auth/2fa/verify   — Verify 2FA code
```

### Assessment Service (`/api/assessments`)
```
GET    /api/assessments              — List all
POST   /api/assessments              — Create
PUT    /api/assessments/{id}         — Update
DELETE /api/assessments/{id}         — Delete
GET    /api/assessments/class/{name} — By class
```

### Grade Service (`/api/grades`)
```
GET  /api/grades                     — List all
POST /api/grades                     — Create
GET  /api/grades/student/{email}     — By student
GET  /api/grades/leaderboard/global  — Global leaderboard
```

### Classes (`/api/classes`)
```
GET    /api/classes      — List all
POST   /api/classes      — Create
DELETE /api/classes/{id} — Delete
```

---

## 📜 License

This project is developed for academic purposes at **Esprit School of Engineering** as part of the PIDEV 4SAE4 module (2025-2026).

---

<div align="center">
  Made with ❤️ by Team 4SAE4 — Esprit School of Engineering
</div>
