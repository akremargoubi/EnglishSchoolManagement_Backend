# 🎓 Esprit PIDEV 4SAE4 — English School Management

**A robust microservices architecture for managing an English language school.**

Developed as part of the **PIDEV — 4th Year Engineering Program** at **Esprit School of Engineering** (Academic Year 2025–2026).

---

## ✨ Project Overview

This backend system manages all operations of an English language school, including:

- Student enrollment and course management
- Assessments and exams
- Learning resources and file uploads
- Scheduling and planning
- Attendance tracking
- Payment processing
- Certificate generation
- Reporting and analytics
- Notifications and messaging

Built using **Spring Boot Microservices**, **Spring Cloud**, and **Docker**.

---

## 🏗️ Architecture

The project follows a **microservices architecture** with:
- API Gateway (Spring Cloud Gateway)
- Eureka Discovery Service
- Multiple independent Spring Boot services
- MySQL databases
- Docker Compose support

---

## 🛠️ Tech Stack

- **Java** 17+
- **Spring Boot** 3.x
- **Spring Cloud** (Gateway, Eureka, OpenFeign)
- **Spring Data JPA** + Hibernate
- **MySQL** 8
- **Maven**
- **Docker** & **Docker Compose**
- Lombok

---

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.8+
- MySQL 8
- Docker & Docker Compose (recommended)

### 1. Clone the Repository
```bash
git clone https://github.com/akremargoubi/EnglishSchoolManagement_Backend.git
cd EnglishSchoolManagement_Backend
```
2. Run with Docker (Recommended)
Bashdocker-compose up --build
3. Manual Run (Development)
Start services in this order:

discovery-service
gateway-service
Then start the other services

The application will be accessible at: http://localhost:8080

📁 Project Structure
```bash
textEnglishSchoolManagement_Backend/
├── discovery-service/
├── gateway-service/
├── esmauthms/
├── course-service/
├── enrollment-service/
├── assessment-service/
├── resources-service/
├── schedule-service/
├── attendance-service/
├── payment_Microsevices/
├── certificate-service/
├── reporting-service/
├── messaging-service/
├── docker-compose.yml
└── README.md
```
🤝 Team Members

Akrem Argoubi (Project Maintainer)
Firas Tourki
Rayen Karouch
Youssef Fadaoui
Kamal Hamdi


📄 License
This project is developed for academic purposes at Esprit School of Engineering - PIDEV 4SAE4 (2025/2026).

Made by the CodeX Team
