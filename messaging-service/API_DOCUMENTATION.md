# Messaging Service API Documentation

## Overview

The **Messaging Service** (`notification-service`) is a Spring Boot microservice that handles email notifications via the Mailjet email API. It is part of the ESM Platform microservices ecosystem and is registered with Eureka Service Discovery.

---

## Service Information

| Property            | Value                          |
|---------------------|--------------------------------|
| **Service Name**    | `notification-service`         |
| **Group / Artifact**| `com.esprit / notificationms`  |
| **Version**         | `0.0.1-SNAPSHOT`               |
| **Framework**       | Spring Boot 3.2.5              |
| **Java Version**    | 17                             |
| **Default Port**    | `2001`                         |
| **Context Path**    | `/api`                         |
| **Database**        | PostgreSQL                     |
| **Email Provider**  | Mailjet (v3.1 Send API)        |

---

## Base URL

```
http://<host>:<port>/api
```

Default local URL:

```
http://localhost:2001/api
```

---

## Configuration Properties

All sensitive configuration is externalized via environment variables with defaults for development.

| Environment Variable          | Property                      | Default Value                                      |
|-------------------------------|-------------------------------|----------------------------------------------------|
| `SERVER_PORT`                 | `server.port`                 | `2001`                                             |
| `EUREKA_CLIENT_ENABLED`       | `eureka.client.enabled`       | `true`                                             |
| `EUREKA_CLIENT_REGISTER_WITH_EUREKA` | `eureka.client.register-with-eureka` | `true`                              |
| `EUREKA_CLIENT_FETCH_REGISTRY`| `eureka.client.fetch-registry`| `true`                                             |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `eureka.client.service-url.defaultZone` | `http://localhost:8761/eureka`          |
| `EUREKA_INSTANCE_INSTANCE_ID` | `eureka.instance.instance-id` | `notification-service:2001`                        |
| `SPRING_DATASOURCE_URL`       | `spring.datasource.url`       | `jdbc:postgresql://localhost:5438/email_db`        |
| `SPRING_DATASOURCE_USERNAME`  | `spring.datasource.username`  | `esm`                                              |
| `SPRING_DATASOURCE_PASSWORD`  | `spring.datasource.password`  | `esmsecret`                                        |
| `MAILJET_API_KEY`             | `mailjet.api-key`             | *(default development key)*                        |
| `MAILJET_SECRET_KEY`          | `mailjet.secret-key`          | *(default development key)*                        |
| `MAILJET_SENDER_EMAIL`        | `mailjet.sender-email`        | `blink@akrem.dev`                                  |
| `MAILJET_SENDER_NAME`         | `mailjet.sender-name`         | `ESM Platform`                                     |

---

## API Endpoints

### 1. Send Email

Sends an email to a specified recipient via the Mailjet API.

```
POST /api/emails/send
```

#### Request

**Headers:**

| Header           | Type   | Required | Description                                      |
|------------------|--------|----------|--------------------------------------------------|
| `Content-Type`   | String | Yes      | Must be `application/json`                       |
| `X-Service-Origin`| String | No      | Identifies which microservice triggered the send |

**Request Body:** `EmailRequest`

| Field     | Type   | Required | Validation    | Description               |
|-----------|--------|----------|---------------|---------------------------|
| `to`      | String | Yes      | `@Email`, `@NotBlank` | Recipient email address |
| `subject` | String | Yes      | `@NotBlank`   | Email subject line        |
| `text`    | String | Yes      | `@NotBlank`   | Plain text email body     |

**Example Request:**

```json
{
  "to": "user@example.com",
  "subject": "Welcome to ESM Platform",
  "text": "Hello! Your account has been created successfully."
}
```

**Example cURL:**

```bash
curl -X POST http://localhost:2001/api/emails/send \
  -H "Content-Type: application/json" \
  -H "X-Service-Origin: auth-service" \
  -d '{
    "to": "user@example.com",
    "subject": "Welcome to ESM Platform",
    "text": "Hello! Your account has been created successfully."
  }'
```

#### Response

**Success — `200 OK`**

```
Email sent successfully
```

**Validation Error — `400 Bad Request`**

```json
{
  "timestamp": "2026-04-29T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "to",
      "message": "must be a well-formed email address"
    }
  ]
}
```

**Server Error — `500 Internal Server Error`**

Returned when Mailjet API call fails.

---

## Data Models

### EmailRequest

DTO used in the request body for sending emails.

```java
public class EmailRequest {
    @Email @NotBlank
    private String to;       // Recipient email address

    @NotBlank
    private String subject;  // Email subject

    @NotBlank
    private String text;     // Plain text body
}
```

### EmailStatus

Enum representing the status of an email.

| Value       | Description                          |
|-------------|--------------------------------------|
| `SENT`      | Email was successfully sent          |
| `FAILED`    | Email sending failed                 |
| `PENDING`   | Email is queued for sending          |
| `CANCELLED` | Email was cancelled before sending   |

### ScheduledEmail (Entity)

Database entity for scheduled/delayed emails. Stored in table `scheduled_emails`.

| Field            | Type              | Nullable | Description                        |
|------------------|-------------------|----------|------------------------------------|
| `id`             | Long              | No       | Auto-generated primary key         |
| `recipient`      | String            | No       | Recipient email address            |
| `subject`        | String            | No       | Email subject                      |
| `text`           | String (TEXT)     | No       | Email body content                 |
| `status`         | EmailStatus       | No       | Current status of the email        |
| `serviceOrigin`  | String            | Yes      | Originating microservice           |
| `scheduledAt`    | LocalDateTime     | No       | When the email should be sent      |
| `createdAt`      | LocalDateTime     | No       | When the record was created        |
| `processedAt`    | LocalDateTime     | Yes      | When the email was actually sent   |

### EmailLog (Entity)

Database entity for email sending logs. Stored in table `email_logs`.

| Field            | Type              | Nullable | Description                        |
|------------------|-------------------|----------|------------------------------------|
| `id`             | Long              | No       | Auto-generated primary key         |
| `recipient`      | String            | No       | Recipient email address            |
| `subject`        | String            | No       | Email subject                      |
| `status`         | EmailStatus       | No       | Result status of the send attempt  |
| `serviceOrigin`  | String            | Yes      | Originating microservice           |
| `errorMessage`   | String            | Yes      | Error details if status is FAILED  |
| `sentAt`         | LocalDateTime     | No       | Timestamp of the send attempt      |

---

## Database Schema

### Tables

```
scheduled_emails
├── id              BIGINT (PK, AUTO_INCREMENT)
├── recipient       VARCHAR (NOT NULL)
├── subject         VARCHAR (NOT NULL)
├── text            TEXT (NOT NULL)
├── status          VARCHAR (NOT NULL) -- ENUM: SENT, FAILED, PENDING, CANCELLED
├── service_origin  VARCHAR
├── scheduled_at    TIMESTAMP (NOT NULL)
├── created_at      TIMESTAMP (NOT NULL)
└── processed_at    TIMESTAMP

email_logs
├── id              BIGINT (PK, AUTO_INCREMENT)
├── recipient       VARCHAR (NOT NULL)
├── subject         VARCHAR (NOT NULL)
├── status          VARCHAR (NOT NULL) -- ENUM: SENT, FAILED, PENDING, CANCELLED
├── service_origin  VARCHAR
├── error_message   VARCHAR
└── sent_at         TIMESTAMP (NOT NULL)
```

---

## External Dependencies

### Mailjet API

The service integrates with the Mailjet v3.1 Send API.

| Property     | Value                           |
|--------------|---------------------------------|
| **Endpoint** | `https://api.mailjet.com/v3.1/send` |
| **Auth**     | Basic Auth (API Key + Secret Key) |
| **Format**   | JSON                            |

**Outbound Request to Mailjet:**

```json
{
  "Messages": [
    {
      "From": {
        "Email": "blink@akrem.dev",
        "Name": "ESM Platform"
      },
      "To": [
        {
          "Email": "user@example.com"
        }
      ],
      "Subject": "Welcome to ESM Platform",
      "TextPart": "Hello! Your account has been created successfully."
    }
  ]
}
```

---

## Swagger / OpenAPI

Interactive API documentation is available at:

| Resource              | URL                                          |
|-----------------------|----------------------------------------------|
| **Swagger UI**        | `http://localhost:2001/api/swagger-ui.html`  |
| **OpenAPI JSON**      | `http://localhost:2001/api/v3/api-docs`      |

Springdoc OpenAPI version: `2.5.0`

---

## Service Discovery

The service registers itself with a Eureka Server.

| Property                   | Value                                |
|----------------------------|--------------------------------------|
| **Eureka Default Zone**    | `http://localhost:8761/eureka`       |
| **Instance ID**            | `notification-service:2001`          |
| **Prefer IP Address**      | `true`                               |

---

## Build & Run

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL running on port 5438 (or configure via env vars)
- Mailjet API credentials

### Build

```bash
mvn clean package
```

### Run

```bash
java -jar target/notificationms-0.0.1-SNAPSHOT.jar
```

Or with Maven:

```bash
mvn spring-boot:run
```

### Docker

```bash
docker build -t notification-service .
docker run -p 2001:2001 \
  -e MAILJET_API_KEY=your_key \
  -e MAILJET_SECRET_KEY=your_secret \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5438/email_db \
  notification-service
```

---

## Project Structure

```
messaging-service/
├── pom.xml
├── Dockerfile
└── src/main/java/com/esprit/notificationms/
    ├── NotificationServiceApplication.java    # Main entry point
    ├── controller/
    │   └── EmailController.java               # REST endpoints
    ├── service/
    │   └── EmailService.java                  # Business logic (Mailjet integration)
    ├── dto/
    │   └── EmailRequest.java                  # Request DTO
    ├── entity/
    │   ├── ScheduledEmail.java                # Scheduled email entity
    │   └── EmailLog.java                      # Email log entity
    └── enums/
        └── EmailStatus.java                   # Status enum
```

---

## Dependencies (pom.xml)

| Dependency                                    | Purpose                     |
|-----------------------------------------------|-----------------------------|
| `spring-boot-starter-web`                     | REST API & embedded Tomcat  |
| `spring-boot-starter-validation`              | Bean validation             |
| `spring-boot-starter-data-jpa`                | JPA / Hibernate ORM         |
| `spring-boot-starter-test`                    | Testing framework           |
| `spring-cloud-starter-netflix-eureka-client`  | Service discovery           |
| `postgresql` (runtime)                        | PostgreSQL JDBC driver      |
| `springdoc-openapi-starter-webmvc-ui`         | Swagger/OpenAPI UI          |
| `lombok`                                      | Boilerplate reduction       |

---

## Future / Planned Endpoints

The following entities exist in the codebase but do not yet have controller endpoints:

| Entity             | Table              | Suggested Endpoints                          |
|--------------------|--------------------|----------------------------------------------|
| `ScheduledEmail`   | `scheduled_emails` | `POST /api/emails/schedule`, `GET /api/emails/scheduled`, `DELETE /api/emails/scheduled/{id}` |
| `EmailLog`         | `email_logs`       | `GET /api/emails/logs`, `GET /api/emails/logs/{id}` |
