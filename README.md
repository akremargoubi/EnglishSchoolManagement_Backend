# 🎓 Esprit PIDEV 4SAE4 — English School Management

> A full-stack microservices application for managing assessments, resources, and planning in an English language school.  
> Developed as part of the **PIDEV — 4th Year Engineering Program** at **Esprit School of Engineering** (Academic Year 2025–2026).

---

## 📌 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Microservices](#microservices)
- [Frontend](#frontend)
- [Features](#features)
- [Getting Started](#getting-started)
- [API Routes](#api-routes)
- [Screenshots](#screenshots)
- [Team](#team)

---

## 📖 Overview

**English School Management** is a platform designed for administrators to manage:
- 📝 **Assessments** — create, edit, publish, and schedule exams, quizzes, and projects
- 📁 **Resources** — upload and link files (PDF, DOCX, Audio, Images) to assessments
- 📅 **Planning** — calendar view of scheduled assessments with timeline
- 📊 **Dashboard** — real-time overview of school activity

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Angular Frontend                  │
│                   localhost:4200                     │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│              API Gateway (Spring Cloud)              │
│                   localhost:8080                     │
│  /api/assessments/** → Assessment Service            │
│  /api/enums/**       → Assessment Service            │
│  /api/planning/**    → Assessment Service            │
│  /api/resources/**   → Resources Service             │
└──────┬──────────────────────────┬───────────────────┘
       │                          │
       ▼                          ▼
┌─────────────┐          ┌─────────────────┐
│  Assessment  │          │    Resources    │
│   Service   │          │     Service     │
│  :8081      │          │     :8096       │
│  MySQL DB   │          │   MySQL DB +    │
│             │          │  File Storage   │
└──────┬──────┘          └────────────────-┘
       │
       ▼
┌─────────────────┐
│  Eureka Server  │
│  (Discovery)    │
│    :8761        │
└─────────────────┘
```

---

## 🛠️ Tech Stack

### Backend
| Technology | Version | Usage |
|---|---|---|
| Java | 23 | Language |
| Spring Boot | 3.2.0 | Framework |
| Spring Cloud Gateway | 4.1.0 | API Gateway + CORS |
| Spring Cloud Netflix Eureka | 4.1.0 | Service Discovery |
| Spring Data JPA | 3.2.0 | ORM |
| Hibernate | 6.3.1 | Database mapping |
| MySQL | 8.x | Database |
| Maven | 3.x | Build tool |

### Frontend
| Technology | Version | Usage |
|---|---|---|
| Angular | 17+ | Framework |
| TypeScript | 5.x | Language |
| RxJS | 7.x | Reactive programming |
| Angular Router | 17+ | Navigation |

---

## ⚙️ Microservices

### 1. 🔍 Discovery Service (Eureka) — `:8761`
- Service registry for all microservices
- Health monitoring and load balancing

### 2. 🌐 Gateway Service — `:8080`
- Single entry point for all API calls
- CORS configuration (centralized)
- Load balancing via Eureka

### 3. 📝 Assessment Service — `:8081`
Handles all assessment and planning logic.

**Endpoints:**
```
GET    /api/assessments          → List all assessments
POST   /api/assessments          → Create assessment
PUT    /api/assessments/{id}     → Update assessment
DELETE /api/assessments/{id}     → Delete assessment
GET    /api/enums/types          → Assessment types (EXAM, QUIZ, PROJECT)
GET    /api/enums/statuses       → Assessment statuses (DRAFT, PUBLISHED, CLOSED)
GET    /api/planning/calendar    → Calendar view by year/month
GET    /api/planning/upcoming    → Upcoming assessments
```

**Entity: Assessment**
```java
id, title, courseName, type, status,
startDate, endDate, duration
```

### 4. 📁 Resources Service — `:8096`
Handles file upload and resource management.

**Endpoints:**
```
POST   /api/resources/upload              → Upload file linked to assessment
GET    /api/resources/assessment/{id}     → Get resources by assessment
DELETE /api/resources/{id}               → Delete resource
```

**Entity: LearningResource**
```java
id, title, type, published, fileUrl, assessmentId
```

---

## 🖥️ Frontend

### Pages
| Route | Component | Description |
|---|---|---|
| `/backoffice/dashboard` | Dashboard | School overview & recent activity |
| `/backoffice/assessments` | Backoffice | Full CRUD for assessments |
| `/backoffice/resources` | Resources | File upload & management |
| `/backoffice/planning` | Planning | Calendar & timeline view |

### Key Features
- **Custom Time Picker** — scroll-based HH:MM selector for scheduling
- **Real-time updates** — no page refresh needed after create/upload/delete
- **Pagination** — 6 items per page with navigation
- **Search & Filter** — by title, course, type, status
- **Notifications** — success/error/confirm popups

---

## ✨ Features

### Assessment Management
- ✅ Create, edit, delete assessments
- ✅ Filter by status (DRAFT / PUBLISHED / CLOSED) and type (EXAM / QUIZ / PROJECT)
- ✅ Schedule with start date, end date, duration
- ✅ Custom scroll time picker for hour/minute selection

### Resource Management
- ✅ Upload PDF, DOCX, Audio, Image files
- ✅ Link resources to specific assessments
- ✅ View & Download files directly
- ✅ Published / Private visibility toggle
- ✅ Real-time list refresh after upload

### Planning
- ✅ Monthly calendar view
- ✅ Timeline sidebar view
- ✅ Navigate between months
- ✅ Click on a day to see assessments

### Dashboard
- ✅ Real-time counts (Assessments, Resources)
- ✅ Recent activity feed
- ✅ Quick action buttons

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- MySQL 8.x
- Maven 3.x
- Angular CLI 17+

### 1. Clone the repository
```bash
git clone https://github.com/firastourki/Esprit-PIDEV-4SAE4-2026-EnglishSchoolManagement.git
cd Esprit-PIDEV-4SAE4-2026-EnglishSchoolManagement
```

### 2. Start MySQL
Create two databases:
```sql
CREATE DATABASE assessment_db;
CREATE DATABASE resources_db;
```

### 3. Start services (in order)

```bash
# 1. Eureka Discovery Service
cd backend/discovery-service
mvn spring-boot:run

# 2. Gateway Service
cd backend/gateway-service
mvn spring-boot:run

# 3. Assessment Service
cd backend/assessment-service
mvn spring-boot:run

# 4. Resources Service
cd backend/resources-service
mvn spring-boot:run
```

### 4. Start Angular Frontend
```bash
cd esm-front
npm install
ng serve
```

### 5. Open in browser
```
http://localhost:4200/backoffice/dashboard
```

---

## 🔌 API Routes Summary

| Method | URL | Description |
|---|---|---|
| GET | `localhost:8080/api/assessments` | All assessments |
| POST | `localhost:8080/api/assessments` | Create assessment |
| PUT | `localhost:8080/api/assessments/{id}` | Update assessment |
| DELETE | `localhost:8080/api/assessments/{id}` | Delete assessment |
| GET | `localhost:8080/api/enums/types` | Assessment types |
| GET | `localhost:8080/api/enums/statuses` | Assessment statuses |
| GET | `localhost:8080/api/planning/calendar?year=2026&month=3` | Calendar |
| POST | `localhost:8080/api/resources/upload` | Upload file |
| GET | `localhost:8080/api/resources/assessment/{id}` | Resources by assessment |
| DELETE | `localhost:8080/api/resources/{id}` | Delete resource |

---

## 👥 Team

| Name | Role | Branch |
|---|---|---|
| Firas Tourki | Backend + Frontend (Assessment, Resources, Planning, Gateway) | `feature-firas-backend` |

---

## 📄 License

This project is developed for academic purposes at **Esprit School of Engineering**.

---

<div align="center">
  <strong>🎓 Esprit School of Engineering — PIDEV 4SAE4 — 2025/2026</strong>
</div>
