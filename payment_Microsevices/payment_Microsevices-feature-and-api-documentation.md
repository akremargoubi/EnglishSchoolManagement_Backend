# Payment Microservice Documentation

## Overview
The **Payment Microservice** is responsible for managing financial transactions within the platform. It handles payment records, provides tracking for student payments related to courses and enrollments, and sends automated notifications upon successful payment creation.

## Technical Stack
- **Framework:** Spring Boot 3.3.5
- **Language:** Java 17
- **Database:** MySQL
- **Service Discovery:** Eureka Client
- **Documentation:** SpringDoc OpenAPI (Swagger UI)
- **Messaging:** Spring Mail (SMTP)
- **Build Tool:** Maven

## Business Logic
- **Payment Lifecycle:**
    - Payments are created with a default status of `PENDING` and the current timestamp if not specified.
    - Upon creation, the system attempts to send an email notification to the administrator/client (currently hardcoded to `kamel.hamdi@esprit.tn` for tracking).
    - Payments are linked to a `studentId`, `courseId`, and `enrollmentId` to maintain traceability across the platform.
- **Validation:**
    - Basic CRUD operations are supported with safety checks (e.g., ensuring new payments don't have a pre-assigned ID).
- **Error Handling:**
    - Email sending failures are logged but do not roll back the payment transaction (currently, though a log error is present in the service).

## Data Model (PaymentEntity)
| Field | Type | Description |
| :--- | :--- | :--- |
| `paymentId` | Long | Primary Key (Auto-increment) |
| `amount` | Double | Transaction amount |
| `method` | String | Payment method (e.g., CARD, CASH, TRANSFER) |
| `status` | String | Transaction status (e.g., PENDING, PAID, FAILED) |
| `date` | LocalDateTime | Timestamp of the transaction |
| `studentId` | Long | ID of the student making the payment |
| `courseId` | Long | ID of the associated course |
| `enrollmentId` | Long | ID of the associated enrollment |

## API Documentation

### Base URL
`http://localhost:8083/api/payments`

### Endpoints

#### 1. Create Payment
- **URL:** `/`
- **Method:** `POST`
- **Description:** Creates a new payment record and sends an email notification.
- **Request Body:** `PaymentEntity` (JSON)
- **Response:** `200 OK` with created `PaymentEntity`.

#### 2. Get All Payments
- **URL:** `/`
- **Method:** `GET`
- **Description:** Retrieves a list of all payment records.
- **Response:** `200 OK` with `List<PaymentEntity>`.

#### 3. Get Payment by ID
- **URL:** `/{id}`
- **Method:** `GET`
- **Description:** Retrieves details of a specific payment by its numeric ID.
- **Response:** `200 OK` with `PaymentEntity` or `404 Not Found`.

#### 4. Update Payment
- **URL:** `/{id}`
- **Method:** `PUT`
- **Description:** Updates an existing payment record.
- **Request Body:** `PaymentEntity` (JSON)
- **Response:** `200 OK` with updated `PaymentEntity`.

#### 5. Delete Payment
- **URL:** `/{id}`
- **Method:** `DELETE`
- **Description:** Removes a payment record from the system.
- **Response:** `204 No Content`.

#### 6. Get Payments by Student
- **URL:** `/by-student/{studentId}`
- **Method:** `GET`
- **Description:** Retrieves all payments associated with a specific student ID.
- **Response:** `200 OK` with `List<PaymentEntity>`.

#### 7. Test Email Configuration
- **URL:** `/test-email`
- **Method:** `GET`
- **Description:** Sends a test email to verify SMTP configuration.
- **Response:** `200 OK` with message "Test email sent".

## Infrastructure Configuration
- **Port:** `8083`
- **Eureka Service Name:** `payment-service`
- **Database Name:** `pidev`
- **SMTP Host:** `smtp.gmail.com` (configurable)

## Swagger UI
API documentation is available at: `http://localhost:8083/swagger-ui.html` (when service is running).
