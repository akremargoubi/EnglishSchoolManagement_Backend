# Messaging Service - Service Documentation

## 1. Overview

The **Messaging Service** (`notification-service`) is a Spring Boot 3.x microservice responsible for sending transactional and notification emails. It acts as a centralized email layer for the ESM Platform, delegating actual delivery to the **Mailjet** email API.

The service is registered with **Eureka Service Discovery**, persists scheduling and logging data to a **PostgreSQL** database, and exposes a RESTful API with **OpenAPI/Swagger** documentation.

---

## 2. Service Metadata

| Property            | Value                               |
|---------------------|-------------------------------------|
| **Service Name**    | `notification-service`              |
| **Artifact**        | `com.esprit:notificationms`         |
| **Version**         | `0.0.1-SNAPSHOT`                    |
| **Framework**       | Spring Boot `3.2.5`                 |
| **Java**            | `17`                                |
| **Port**            | `2001` (configurable via `SERVER_PORT`) |
| **Context Path**    | `/api`                              |
| **Eureka Instance** | `notification-service:2001`         |

---

## 3. Technology Stack

| Layer           | Technology                          |
|-----------------|-------------------------------------|
| **Language**    | Java 17                             |
| **Framework**   | Spring Boot 3.2.5                   |
| **Build**       | Maven                               |
| **Web**         | Spring Web MVC (embedded Tomcat)    |
| **ORM**         | Spring Data JPA / Hibernate         |
| **Database**    | PostgreSQL                          |
| **Discovery**   | Netflix Eureka Client               |
| **Email API**   | Mailjet v3.1                        |
| **Docs**        | Springdoc OpenAPI (Swagger) 2.5.0   |
| **Lombok**      | Boilerplate reduction               |
| **Validation**  | Jakarta Bean Validation             |

---

## 4. Configuration

### 4.1 Environment Variables

| Variable                                | Description                        | Default                                        |
|-----------------------------------------|------------------------------------|------------------------------------------------|
| `SERVER_PORT`                           | HTTP server port                   | `2001`                                         |
| `EUREKA_CLIENT_ENABLED`                 | Enable Eureka client               | `true`                                         |
| `EUREKA_CLIENT_REGISTER_WITH_EUREKA`    | Register with Eureka               | `true`                                         |
| `EUREKA_CLIENT_FETCH_REGISTRY`          | Fetch registry from Eureka         | `true`                                         |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`  | Eureka server URL                  | `http://localhost:8761/eureka`                 |
| `EUREKA_INSTANCE_INSTANCE_ID`           | Eureka instance identifier         | `notification-service:2001`                    |
| `SPRING_DATASOURCE_URL`                 | JDBC connection URL                | `jdbc:postgresql://localhost:5438/email_db`    |
| `SPRING_DATASOURCE_USERNAME`            | Database username                  | `esm`                                          |
| `SPRING_DATASOURCE_PASSWORD`            | Database password                  | `esmsecret`                                    |
| `MAILJET_API_KEY`                       | Mailjet public API key             | *(development default)*                        |
| `MAILJET_SECRET_KEY`                    | Mailjet secret API key             | *(development default)*                        |
| `MAILJET_SENDER_EMAIL`                  | Default sender email               | `blink@akrem.dev`                              |
| `MAILJET_SENDER_NAME`                   | Default sender display name        | `ESM Platform`                                 |

### 4.2 JPA / Database

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=false
```

### 4.3 Swagger

| Resource              | Path                              |
|-----------------------|-----------------------------------|
| **Swagger UI**        | `/api/swagger-ui.html`            |
| **OpenAPI Spec**      | `/api/v3/api-docs`                |

---

## 5. API Endpoints

### 5.1 Send Email

```
POST /api/emails/send
```

Sends a plain-text email to a single recipient via Mailjet.

**Request Headers:**

| Header             | Required | Description                              |
|--------------------|----------|------------------------------------------|
| `Content-Type`     | Yes      | `application/json`                       |
| `X-Service-Origin` | No       | Name of the calling microservice         |

**Request Body:**

```json
{
  "to": "string (email, required)",
  "subject": "string (required)",
  "text": "string (required)"
}
```

| Field     | Type   | Constraints              | Description              |
|-----------|--------|--------------------------|--------------------------|
| `to`      | String | `@Email`, `@NotBlank`    | Recipient email address  |
| `subject` | String | `@NotBlank`              | Email subject line       |
| `text`    | String | `@NotBlank`              | Plain text body content  |

**Response:**

| Status | Body                              |
|--------|-----------------------------------|
| `200`  | `"Email sent successfully"`       |
| `400`  | Validation error details          |
| `500`  | Internal server error             |

**Example:**

```bash
curl -X POST http://localhost:2001/api/emails/send \
  -H "Content-Type: application/json" \
  -d '{"to":"john@example.com","subject":"Hello","text":"Hi John!"}'
```

---

## 6. Data Models

### 6.1 EmailRequest (DTO)

```java
@Data
public class EmailRequest {
    @Email @NotBlank  private String to;
    @NotBlank         private String subject;
    @NotBlank         private String text;
}
```

### 6.2 EmailStatus (Enum)

```java
public enum EmailStatus {
    SENT, FAILED, PENDING, CANCELLED
}
```

### 6.3 ScheduledEmail (Entity)

Table: `scheduled_emails`

| Column          | Type          | Constraints     | Description                     |
|-----------------|---------------|-----------------|---------------------------------|
| `id`            | Long          | PK, Identity    | Auto-generated ID               |
| `recipient`     | String        | NOT NULL        | Recipient email                 |
| `subject`       | String        | NOT NULL        | Email subject                   |
| `text`          | String (TEXT) | NOT NULL        | Email body                      |
| `status`        | EmailStatus   | NOT NULL        | PENDING / SENT / FAILED / CANCELLED |
| `serviceOrigin` | String        | Nullable        | Originating service             |
| `scheduledAt`   | LocalDateTime | NOT NULL        | Target send time                |
| `createdAt`     | LocalDateTime | NOT NULL        | Record creation time            |
| `processedAt`   | LocalDateTime | Nullable        | Actual processing time          |

### 6.4 EmailLog (Entity)

Table: `email_logs`

| Column          | Type          | Constraints     | Description                     |
|-----------------|---------------|-----------------|---------------------------------|
| `id`            | Long          | PK, Identity    | Auto-generated ID               |
| `recipient`     | String        | NOT NULL        | Recipient email                 |
| `subject`       | String        | NOT NULL        | Email subject                   |
| `status`        | EmailStatus   | NOT NULL        | Send result status              |
| `serviceOrigin` | String        | Nullable        | Originating service             |
| `errorMessage`  | String        | Nullable        | Error details on failure        |
| `sentAt`        | LocalDateTime | NOT NULL        | Timestamp of send attempt       |

---

## 7. Database Schema

```sql
-- scheduled_emails
CREATE TABLE scheduled_emails (
    id              BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    recipient       VARCHAR(255) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    text            TEXT         NOT NULL,
    status          VARCHAR(50)  NOT NULL,  -- SENT, FAILED, PENDING, CANCELLED
    service_origin  VARCHAR(255),
    scheduled_at    TIMESTAMP    NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    processed_at    TIMESTAMP
);

-- email_logs
CREATE TABLE email_logs (
    id              BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    recipient       VARCHAR(255) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    status          VARCHAR(50)  NOT NULL,  -- SENT, FAILED, PENDING, CANCELLED
    service_origin  VARCHAR(255),
    error_message   VARCHAR(255),
    sent_at         TIMESTAMP    NOT NULL
);
```

---

## 8. Architecture

### 8.1 Component Flow

```
[Client / Microservice]
         |
         | POST /api/emails/send
         v
[EmailController] ─── validates EmailRequest
         |
         v
[EmailService] ───── builds Mailjet payload
         |              authenticates with Basic Auth
         |
         v
[Mailjet API] (https://api.mailjet.com/v3.1/send)
         |
         v
    [Response]
```

### 8.2 Service Integration

| Integration         | Type     | Direction | Details                              |
|---------------------|----------|-----------|--------------------------------------|
| **Eureka Server**   | HTTP     | Outbound  | Registration & heartbeat             |
| **PostgreSQL**      | TCP/JDBC | Outbound  | JPA entity persistence               |
| **Mailjet API**     | HTTPS    | Outbound  | Email delivery                       |
| **Other Services**  | HTTP     | Inbound   | Receive email send requests          |

---

## 9. Project Structure

```
messaging-service/
├── pom.xml                                     # Maven build config
├── Dockerfile                                  # Container definition
├── API_DOCUMENTATION.md                        # API reference
├── SERVICE_DOCUMENTATION.md                    # This file
│
└── src/main/java/com/esprit/notificationms/
    ├── NotificationServiceApplication.java     # Main class (@EnableScheduling)
    ├── controller/
    │   └── EmailController.java                # REST controller
    ├── service/
    │   └── EmailService.java                   # Mailjet integration logic
    ├── dto/
    │   └── EmailRequest.java                   # Request DTO
    ├── entity/
    │   ├── ScheduledEmail.java                 # Scheduled email JPA entity
    │   └── EmailLog.java                       # Email log JPA entity
    └── enums/
        └── EmailStatus.java                    # Status enum

src/main/resources/
└── application.properties                      # Application configuration
```

---

## 10. Build & Deployment

### 10.1 Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL (default port 5438, database `email_db`)
- Mailjet account with API credentials

### 10.2 Local Build

```bash
mvn clean package -DskipTests
```

### 10.3 Local Run

```bash
java -jar target/notificationms-0.0.1-SNAPSHOT.jar
```

Or via Maven:

```bash
mvn spring-boot:run
```

### 10.4 Docker

**Build:**

```bash
docker build -t notification-service .
```

**Run:**

```bash
docker run -d \
  --name notification-service \
  -p 2001:2001 \
  -e SERVER_PORT=2001 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5438/email_db \
  -e SPRING_DATASOURCE_USERNAME=esm \
  -e SPRING_DATASOURCE_PASSWORD=esmsecret \
  -e MAILJET_API_KEY=your_api_key \
  -e MAILJET_SECRET_KEY=your_secret_key \
  -e MAILJET_SENDER_EMAIL=your@email.com \
  -e MAILJET_SENDER_NAME="ESM Platform" \
  notification-service
```

### 10.5 Docker Compose (standalone)

```yaml
services:
  notification-service:
    image: notification-service:latest
    ports:
      - "2001:2001"
    environment:
      SERVER_PORT: 2001
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/email_db
      SPRING_DATASOURCE_USERNAME: esm
      SPRING_DATASOURCE_PASSWORD: esmsecret
      MAILJET_API_KEY: ${MAILJET_API_KEY}
      MAILJET_SECRET_KEY: ${MAILJET_SECRET_KEY}
      MAILJET_SENDER_EMAIL: ${MAILJET_SENDER_EMAIL}
      MAILJET_SENDER_NAME: ESM Platform
    depends_on:
      - postgres

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: email_db
      POSTGRES_USER: esm
      POSTGRES_PASSWORD: esmsecret
    ports:
      - "5438:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

---

## 11. Logging

```properties
logging.level.root=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

Logs include timestamps in `yyyy-MM-dd HH:mm:ss` format at INFO level and above.

---

## 12. Health & Actuator

Spring Boot Actuator is **not** currently included in the dependencies. To add health check endpoints, include:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Then expose endpoints:

```properties
management.endpoints.web.exposure.include=health,info
```

---

## 13. Known Limitations

| Limitation                         | Status        |
|------------------------------------|---------------|
| Only plain-text emails (no HTML)   | Current       |
| Single recipient per request       | Current       |
| No attachments support             | Current       |
| Scheduled email endpoints not implemented | Entities exist, no controller |
| Email log query endpoints not implemented | Entities exist, no controller |
| No retry mechanism on Mailjet failure | Current    |
| No rate limiting                   | Current       |

---

## 14. Planned Enhancements

1. **Scheduled Email API** — Expose CRUD endpoints for `ScheduledEmail` with a scheduler task.
2. **Email Log API** — Expose query endpoints for `EmailLog` for auditing.
3. **HTML Email Support** — Add `HtmlPart` field to Mailjet payload.
4. **Multiple Recipients** — Support CC, BCC, and bulk sends.
5. **Attachment Support** — Handle file attachments in email requests.
6. **Retry & Circuit Breaker** — Add resilience4j for Mailjet API failures.
7. **Spring Boot Actuator** — Add `/actuator/health` and metrics endpoints.
8. **Rate Limiting** — Protect against email abuse via bucket4j or gateway-level limits.
