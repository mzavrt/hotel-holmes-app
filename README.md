# Holmes Hotel - Omnichannel Integration Gateway

**A modular, enterprise-grade integration platform that unifies multiple booking channels (JSON/REST, XML/REST, CSV files) into a standardized BPMN workflow orchestrated by Camunda 8 (Zeebe).**

---

## Table of Contents

- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Key Technologies](#key-technologies)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Webhook Configuration with ngrok](#webhook-configuration-with-ngrok)
- [Enterprise Integration Patterns (EIP)](#enterprise-integration-patterns-eip)
- [Zeebe JobWorkers](#zeebe-jobworkers)
- [Email Notification System](#email-notification-system)
- [Database & Persistence](#database--persistence)
- [API Testing & Webhooks](#api-testing--webhooks)
- [Building & Testing](#building--testing)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [Glossary](#glossary)

---

## Project Overview

**Holmes Hotel** is a cloud-native hotel management system that integrates booking requests from multiple external sources into a unified BPMN process engine. The system:

- **Accepts bookings** from JSON/REST APIs, XML/REST APIs, and CSV file uploads
- **Normalizes data** across all channels using Apache Camel's Enterprise Integration Patterns
- **Orchestrates workflows** using Camunda 8 (Zeebe) BPMN diagrams
- **Executes asynchronous tasks** via Zeebe JobWorkers (payments, email notifications, in-room services)
- **Persists data** to H2 database for local testing and development

**Use Cases:**
- Multi-channel booking aggregation
- Real-time workflow orchestration
- Asynchronous task execution (email, payment processing)
- Data normalization from heterogeneous sources

---

## Architecture

### High-Level System Design

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          OMNICHANNEL BOOKING SOURCES                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌──────────────────┐    ┌──────────────────┐    ┌─────────────────────┐   │
│  │  JSON REST API   │    │   XML REST API   │    │  CSV File Polling   │   │
│  │  camel/webhook/json   │    │ camel/webhook/xml    │    │  data/inbox/*.csv   │   │
│  └────────┬─────────┘    └────────┬─────────┘    └─────────┬───────────┘   │
│           │                       │                         │                │
│           └───────────────────────┼─────────────────────────┘                │
│                                   ▼                                           │
│           ┌─────────────────────────────────────────────┐                   │
│           │  APACHE CAMEL INTEGRATION GATEWAY           │                   │
│           │  (Normalization, Message Routing, EIP)      │                   │
│           │                                              │                   │
│           │  ┌──────────────────────────────────────┐   │                   │
│           │  │ Camel Routes:                        │   │                   │
│           │  │ • JsonWebhookRoute                   │   │                   │
│           │  │ • XmlWebhookRoute                    │   │                   │
│           │  │ • CsvFileRoute                       │   │                   │
│           │  │ • MessageEndEventEmailRoute          │   │                   │
│           │  │ • CamundaStarterRoute                │   │                   │
│           │  └──────────────────────────────────────┘   │                   │
│           │                                              │                   │
│           │  EIP Patterns:                               │                   │
│           │  • Content-Based Router                     │                   │
│           │  • Message Transformer                      │                   │
│           │  • Aggregator (convergence point)           │                   │
│           │  • Polling Consumer (CSV)                   │                   │
│           │  • Service Activator                        │                   │
│           └─────────────────────────────────────────────┘                   │
│                                   ▼                                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
                    ▼                               ▼
      ┌──────────────────────────┐    ┌─────────────────────────┐
      │   CAMUNDA 8 (ZEEBE)       │    │   H2 DATABASE           │
      │   Process Engine          │    │   (Local Persistence)   │
      │   (SaaS Cloud)            │    │                         │
      │                           │    │  Tables:                │
      │  BPMN Workflows:          │    │  • Reservations         │
      │  • Msg_NewBooking         │    │  • Rooms                │
      │  • Msg_NewComplaint       │    │  • ServiceOrders        │
      │  • Msg_NewServiceOrder    │    │  • MarketRates          │
      │                           │    │  • Incidents            │
      │  JobWorkers:              │    │                         │
      │  • PaymentWorker          │    └─────────────────────────┘
      │  • BookingWorker          │
      │  • CheckInWorker          │
      │  • ComplaintWorker        │
      │  • EmailMessageEventWorker│
      └──────────────────────────┘
```

### Data Flow: From Booking Source to BPMN Execution

1. **Booking arrives** via JSON/REST, XML/REST, or CSV file polling
2. **Camel normalizes** keys and enriches with metadata (source, correlationKey)
3. **Message published** to Camunda with `Msg_NewBooking` event
4. **BPMN workflow** executes with XOR gateways and JobWorker tasks
5. **Database persisted** to H2 with booking data

---

## Key Technologies

| Technology | Version | Purpose |
|-----------|---------|---------|
| **Java** | 25 | Modern Java runtime |
| **Spring Boot** | 4.0.6 | Application framework |
| **Apache Camel** | 4.20.0 | Integration routing & EIP |
| **Camunda 8 (Zeebe)** | 8.9.5 | BPMN process engine (SaaS) |
| **H2 Database** | Latest | In-memory database |

---

## Project Structure

```
hotel-holmes-app/
│
├── src/main/java/com/holmeshotel/
│   │
│   ├── HolmesHotelApp.java
│   │   └─ Main Spring Boot application entry point
│   │
│   ├── controller/
│   │   ├─ MockKDSController.java       (Mock Kitchen Display System)
│   │   ├─ MockPaymentController.java   (Mock Payment Gateway)
│   │   └─ WebhookController.java       (API for firing Message Events into Camunda)
│   │
│   ├── integration/
│   │   ├── camel/
│   │   │   ├─ JsonWebhookRoute.java           REST JSON endpoint & normalization
│   │   │   ├─ XmlWebhookRoute.java            REST XML endpoint + XPath extraction
│   │   │   ├─ XPathDataExtractor.java         Spring Bean for XPath data extraction
│   │   │   ├─ CsvFileRoute.java               CSV file polling & parsing
│   │   │   ├─ CamundaStarterRoute.java        Central Zeebe message publisher
│   │   │   └─ MessageEndEventEmailRoute.java  Email notification routing
│   │  
│   │
│   ├── entity/
│   │   ├─ Reservation.java              (JPA Entity - Booking records)
│   │   ├─ Room.java                     (JPA Entity - Hotel rooms)
│   │   ├─ ServiceOrder.java             (JPA Entity - In-room service requests)
│   │   ├─ MarketRate.java               (JPA Entity - Pricing model)
│   │   └─ Incident.java                 (JPA Entity - Complaint tickets)
│   │
│   ├── repository/
│   │   ├─ ReservationRepository.java
│   │   ├─ RoomRepository.java
│   │   ├─ ServiceOrderRepository.java
│   │   ├─ MarketRateRepository.java
│   │   └─ IncidentRepository.java
│   │
│   └── worker/
│       ├─ BookingWorker.java            (Business logic for booking persistence)
│       ├─ CheckInWorker.java
│       ├─ ComplaintWorker.java
        ├─ EmailMessageEventWorker.java
│       ├─ InRoomServiceWorker.java
│       ├─ PaymentWorker.java
│       └─ PricingWorker.java
│
├── src/main/resources/
│   ├── application.properties            (Spring Boot + Camel + Camunda config)
│   ├── data.sql                          (H2 initialization script)
│   └── static/                           (Static web assets)
│
├── data/
│   ├── inbox/                            (CSV file polling directory)
│   │   └─ sample_bookings.csv
│   ├── error/                            (Failed file error directory)
│   └── sample_booking.xml
│
├── pom.xml                               (Maven build configuration)
│
└── README.md                             (This file)
```

### Package Responsibilities

#### `/integration/camel` — Apache Camel Routes
**Purpose**: Multi-channel message normalization and routing

| Class | Responsibility |
|-------|-----------------|
| `JsonWebhookRoute` | REST JSON endpoint, key transformation, convergence routing |
| `XmlWebhookRoute` | REST XML endpoint, XPath-based data extraction |
| `XPathDataExtractor` | Spring Bean with XPath expressions for XML parsing |
| `CsvFileRoute` | File polling, CSV parsing, record splitting |
| `CamundaStarterRoute` | Central hub: publishes Zeebe messages to Camunda |
| `MessageEndEventEmailRoute` | Content-Based Router for email notifications |

#### `/worker` — JobWorkers
**Purpose**: Asynchronous task execution for long-running processes

| Class | Responsibility |
|-------|-----------------|
| `EmailMessageEventWorker` | Handles email sending events from BPMN message end events |
| `PaymentWorker` | Payment processing (calls mock gateway or real processor) |
| `BookingWorker` | Persists reservation to H2 database |
| `CheckInWorker` | Manages guest check-in workflow |
| `ComplaintWorker` | Manages complaint/incident lifecycle |
| `InRoomServiceWorker` | Manages in-room service order coordination |

#### `/entity` — JPA Entities
**Purpose**: Object-relational mapping for database persistence

| Class | Table | Responsibility |
|-------|-------|-----------------|
| `Reservation` | `reservations` | Booking records with guest & room details |
| `Room` | `rooms` | Hotel room inventory & availability |
| `ServiceOrder` | `service_orders` | In-room service requests |
| `MarketRate` | `market_rates` | Dynamic pricing data |
| `Incident` | `incidents` | Complaint/issue tickets |

---

## Getting Started

### Prerequisites

- **Java 25** or higher
- **Maven 3.8+**
- **ngrok** (for exposing the local mock API services to Camunda SaaS)
- **Git**

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/mzavrt/hotel-holmes-app
   cd hotel-holmes-app
   ```

2. **Build the project**
   ```bash
   mvn clean package -DskipTests
   ```

3. **Configure Camunda credentials** (optional for local testing)
   
   Edit `src/main/resources/application.properties`:
   ```properties
   camunda.client.mode=saas
   camunda.client.auth.client-id=YOUR_CLIENT_ID
   camunda.client.auth.client-secret=YOUR_CLIENT_SECRET
   camunda.client.cloud.cluster-id=YOUR_CLUSTER_ID
   camunda.client.cloud.region=fra-1
   ```

4. **Start the application**
   ```bash
   mvn spring-boot:run
   ```

   The application will start on `http://localhost:8080`

### Quick Test

1. **Test the JSON endpoint**
   ```bash
   curl -X POST http://localhost:8080/camel/api/webhook/json \
     -H "Content-Type: application/json" \
     -d '{
       "guestName": "John Doe",
       "email": "john@example.com",
       "requestedRoomType": "deluxe",
       "checkInDate": "2026-06-15",
       "checkOutDate": "2026-06-18"
     }'
   ```

2. **Expected Response**
   ```json
   {
     "status": "Accepted",
     "message": "Booking request received and injected into process engine",
     "correlationKey": "BOOKING-550e8400-e29b-41d4-a716-446655440000",
     "processMessage": "Msg_NewBooking",
     "source": "JSON API Gateway"
   }
   ```

## API Endpoints

### REST Webhook Endpoints

| Endpoint | Method | Content-Type | Purpose |
|----------|--------|--------------|---------|
| `/api/camel/webhook/json` | POST | application/json | Accept JSON booking requests |
| `/api/camel/webhook/xml` | POST | application/xml | Accept XML booking requests |

### JSON Booking Request
```bash
curl -X POST http://localhost:8080/camel/api/webhook/json \
  -H "Content-Type: application/json" \
  -d '{
    "guestName": "Jane Smith",
    "email": "jane@example.com",
    "requestedRoomType": "Suite",
    "checkInDate": "2026-06-20",
    "checkOutDate": "2026-06-23",
    "amenitiesDetails": "Late checkout"
  }'
```

### CSV File Polling

Place CSV files in `data/inbox/`:
```
guestName;email;phoneNumber;requestedRoomType;checkInDate;checkOutDate;cardNumber;cardExpiry;amenitiesDetails
John Doe;john@example.com;+1111111111;deluxe;2024-07-01;2024-07-03;4111111111111111;07/25;Extra towels
```

The route will poll every 5 seconds, parse each line, and send to Camunda as separate messages.

---

## Webhook Configuration with ngrok

### Why ngrok?

This project includes mock services (`MockKDSController`, `MockPaymentController`) that Camunda 8 SaaS calls during BPMN workflow execution (e.g., to simulate payment processing or kitchen display updates). Since Camunda SaaS runs in the cloud, it cannot reach `localhost` directly. **ngrok** creates a public HTTPS tunnel to your local application so Camunda can invoke these mock endpoints.

> **Note:** ngrok is **not** required for the booking webhook endpoints (`/camel/api/webhook/*`). Those are called by external clients (e.g., Postman) that connect to your local machine directly.

### Setup ngrok

1. **Install ngrok**
   ```bash
   # macOS
   brew install ngrok

   # Windows (using Chocolatey)
   choco install ngrok

   # Or download from https://ngrok.com/download
   ```

2. **Authenticate ngrok** (optional but recommended)
   ```bash
   ngrok config add-authtoken YOUR_AUTH_TOKEN
   ```

3. **Expose port 8080**
   ```bash
   ngrok http 8080
   ```

   Output:
   ```
   ngrok                                       (Ctrl+C to quit)

   Session Status                       online
   Account                              your-email@example.com
   Version                              3.x.x
   Region                               United States (us)
   Latency                              45ms
   Web Interface                        http://127.0.0.1:4040
   Forwarding                           https://abc123.ngrok.io -> http://localhost:8080

   Connections                          ttl    opn    rt1    rt5    p50    p95
                                         0      0      0.00   0.00   0.00   0.00
   ```

4. **Configure Camunda BPMN**

   In your BPMN diagram's REST connector task (for mock service calls):
   
   ```
   Endpoint: https://abc123.ngrok.io/mock/payment
   Method: POST
   Headers:
   - Content-Type: application/json
   ```

   This URL is now publicly accessible to Camunda SaaS!

### Security Notes

- ⚠️ ngrok URLs are **public** — treat as development-only
- ✅ Use HTTPS (ngrok automatically provides SSL)
- ✅ Authenticate ngrok with your account token
- ✅ For production, use a dedicated API Gateway (Kong, AWS API Gateway, etc.)

---

## Enterprise Integration Patterns (EIP)

Holmes Hotel implements several proven Enterprise Integration Patterns:

### 1. Content-Based Router
**Location**: `MessageEndEventEmailRoute` (email routing)

Routes messages based on message content (e.g., `messageName` field):
```java
.choice()
  .when().simple("${body[messageName]} == 'RESERVATION_DENIED'")
    // Handle denied reservation
  .when().simple("${body[messageName]} == 'BOOKING_CONFIRMED'")
    // Handle confirmed booking
.end()
```

### 2. Message Transformer
**Location**: `JsonWebhookRoute`, `XmlWebhookRoute`

Normalizes heterogeneous input data:
```java
// Before: requestedRoomType
payload.put("roomType", payload.remove("requestedRoomType"));

// Before: amenitiesDetails
payload.put("guestPreferences", payload.remove("amenitiesDetails"));
```

### 3. Aggregator (Convergence)
**Location**: `CamundaStarterRoute`

All three channels (JSON, XML, CSV) converge to a single route:
```
JsonWebhookRoute  ──┐
XmlWebhookRoute   ──┼──> direct:startCamunda ──> Camunda
CsvFileRoute      ──┘
```

### 4. Polling Consumer
**Location**: `CsvFileRoute`

Automatically polls filesystem for CSV files:
```java
from("file://data/inbox?include=.*\\.csv$&delay=5000")
  // Process files every 5 seconds
```

### 5. Splitter
**Location**: `CsvFileRoute`

Splits CSV file into individual records:
```java
.split(body().convertToString().split("\n"))
  // Process each line separately
```

### 6. Service Activator
**Location**: `EmailMessageEventWorker`, `CamundaStarterRoute`

Bridges to external services:
```java
producerTemplate.sendBody("direct:mockEmailRouter", processVariables);
camundaClient.newPublishMessageCommand().send().join();
```

### 7. Message Enricher
**Location**: `MessageEndEventEmailRoute`

Adds data to messages:
```java
// Add subject and body to booking data
body.put("subject", "Booking Confirmation - Welcome to Holmes Hotel");
body.put("body", "Dear " + guestName + "...");
```

---

## Zeebe JobWorkers

Zeebe JobWorkers are Spring `@Component` classes that execute asynchronous tasks triggered by BPMN service tasks.

**How it works:**
1. BPMN service task is executed with type (e.g., `"email-sender"`)
2. Zeebe engine creates a job
3. Available JobWorker claims and executes it
4. JobWorker completes the job, BPMN process continues

### Example: EmailMessageEventWorker

```java
@Component
public class EmailMessageEventWorker {
  
  @JobWorker(type = "email-sender", autoComplete = true)
  public void sendEmailMessage(ActivatedJob job) {
    String messageName = job.getCustomHeaders().get("messageName");
    Map<String, Object> variables = job.getVariablesAsMap();
    variables.put("messageName", messageName);
    producerTemplate.sendBody("direct:mockEmailRouter", variables);
  }
}
```

### Available JobWorkers

| JobWorker | Task Type | Purpose | Auto-Complete |
|-----------|-----------|---------|---------------|
| `EmailMessageEventWorker` | `email-sender` | Send email notifications | ✅ true |
| `PaymentWorker` | `payment-processor` | Process credit card payments | ✅ true |
| `BookingWorker` | `booking-writer` | Persist booking to database | ✅ true |
| `CheckInWorker` | `check-in-processor` | Execute check-in workflow | ✅ true |
| `ComplaintWorker` | `complaint-handler` | Create incident ticket | ✅ true |
| `InRoomServiceWorker` | `service-order-handler` | Coordinate service requests | ✅ true |

---

## Email Notification System

**Mock Email Workflow:**
1. BPMN message end event triggers service task `"email-sender"`
2. `EmailMessageEventWorker` receives job and sends to Camel
3. `MessageEndEventEmailRoute` uses Content-Based Router to handle 4 cases
4. Console logs formatted ASCII email box

**Email Cases:**

| Message Name | Subject | Trigger |
|--------------|---------|---------|
| `RESERVATION_DENIED` | "Reservation Update - Holmes Hotel" | Room unavailable |
| `DETAILS_REJECTED` | "Action Required: Issue with your Holmes Hotel Booking" | Validation failed |
| `PAYMENT_FAILED` | "Payment Failed - Holmes Hotel Reservation" | Payment declined |
| `BOOKING_CONFIRMED` | "Booking Confirmation - Welcome to Holmes Hotel" | Successful booking |

---

## Database & Persistence

### H2 Database Configuration

H2 is an in-memory relational database ideal for development and testing:

```properties
# src/main/resources/application.properties
spring.datasource.url=jdbc:h2:mem:hoteldb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

### Access H2 Console

While the application is running:
```
http://localhost:8080/h2-console
```

**Connection Details:**
- **JDBC URL**: `jdbc:h2:mem:hoteldb`
- **User**: `sa`
- **Password**: (leave blank)

### Database Schema

Tables are automatically created by Hibernate:

| Table | Entity | Purpose |
|-------|--------|---------|
| `reservations` | `Reservation` | Booking records |
| `rooms` | `Room` | Room inventory |
| `service_orders` | `ServiceOrder` | In-room service requests |
| `market_rates` | `MarketRate` | Pricing data |
| `incidents` | `Incident` | Complaints/issues |

---

## API Testing & Webhooks

To test the integration and trigger BPMN processes, import the provided Postman collection.

### Setup

1. Open **Postman**.
2. Click **Import** in the top left corner.
3. Select the file: `postman/holmes-hotel-api.postman_collection.json`.
4. Ensure your local Spring Boot application is running on port `8080`.

### Available Collections

The collection includes pre-configured requests for all entry points:

| Request Name | Endpoint | Description |
| :--- | :--- | :--- |
| **New Booking (JSON)** | `POST /camel/api/webhook/json` | Starts `Msg_NewBooking` process |
| **New Booking (XML)** | `POST /camel/api/webhook/xml` | Starts `Msg_NewBooking` process via XML |
| **New Complaint** | `POST /camel/api/webhook/complaint` | Starts `Msg_NewComplaint` process |
| **Service Order** | `POST /camel/api/webhook/service-order` | Starts `ServiceOrderReceived` process |

*Note: If you are running Camunda SaaS, ensure your local mock API controllers (`MockPaymentController`, `MockKDSController`) are reachable via your **ngrok** tunnel (see [Webhook Configuration with ngrok](#webhook-configuration-with-ngrok)).*

---

## Building & Testing

### Build Commands

```bash
# Clean and build (skipping tests)
mvn clean package -DskipTests

# Build and run tests
mvn clean package

# Run tests only
mvn test
```

### Running the Application

```bash
# Spring Boot Maven plugin
mvn spring-boot:run

# Or use the compiled JAR
java -jar target/hotel-holmes-app-0.0.1-SNAPSHOT.jar
```

### Testing the Endpoints

**Test JSON Endpoint:**
```bash
curl -X POST http://localhost:8080/camel/api/webhook/json \
  -H "Content-Type: application/json" \
  -d '{"guestName":"Test","email":"test@test.com"}'
```

**Test XML Endpoint:**
```bash
curl -X POST http://localhost:8080/camel/api/webhook/xml \
  -H "Content-Type: application/xml" \
  -d '<booking><guestName>Test</guestName></booking>'
```

**Test CSV Processing:**
```bash
# Copy sample CSV to inbox
cp data/sample_bookings.csv data/inbox/test_$(date +%s).csv

# Wait 5 seconds for polling cycle
sleep 5

# Check Camunda for new process instances
```

---

## Configuration

### application.properties

All configuration is centralized in `application.properties`:

### Environment Variables (Optional)

For sensitive data, use environment variables:

```bash
export CAMUNDA_CLIENT_ID="your-client-id"
export CAMUNDA_CLIENT_SECRET="your-client-secret"
```

Then reference in properties:
```properties
camunda.client.auth.client-id=${CAMUNDA_CLIENT_ID}
camunda.client.auth.client-secret=${CAMUNDA_CLIENT_SECRET}
```

---

## Troubleshooting

### Issue: "Cannot resolve import org.springframework"

**Cause**: Maven hasn't downloaded dependencies

**Solution**:
```bash
mvn clean install -DskipTests
mvn eclipse:clean eclipse:eclipse  # if using Eclipse
```

### Issue: CSV files not being processed

**Cause**: File polling not finding files or delay not elapsed

**Solution**:
```bash
# Ensure files are in correct directory
ls -la data/inbox/

# Check logs for polling messages
tail -f application.log | grep "CSV"

# Increase delay in CsvFileRoute if needed
from("file://data/inbox?include=.*\\.csv$&delay=10000")  // 10 seconds
```

### Issue: Camunda JobWorker not receiving jobs

**Cause**: JobWorker type doesn't match BPMN task type, or credentials incorrect

**Solution**:
1. Check BPMN task type matches `@JobWorker(type = "...")`
2. Verify Camunda credentials in `application.properties`
3. Check network connectivity to Camunda SaaS
4. Review JobWorker logs for connection errors

### Issue: ngrok tunnel not connecting to localhost

**Cause**: Application not running or port mismatch

**Solution**:
```bash
# Ensure app is running on 8080
lsof -i :8080

# Check ngrok is pointing to correct port
ngrok http 8080

# Test local connectivity
curl http://localhost:8080/camel/api/webhook/json
```

### Issue: Email not printing to console

**Cause**: Wrong messageName, missing body field, or logging level

**Solution**:
```bash
# Enable DEBUG logging
logging.level.com.holmeshotel=DEBUG

# Check messageName value in logs
# Ensure all required process variables present
```

---


## Contributing

- Follow Google Java Style Guide
- Add comments explaining EIP patterns
- Keep routes modular and single-purpose
- To add a new channel: Create `YourRoute.java` → normalize to Map → route to `direct:startCamunda`

---

## Glossary

### Message End Event

BPMN diagram element that signals completion of a booking flow and triggers an action (e.g., send email):
```
Msg_ReservationDenied    → Send "Reservation Update" email
Msg_PaymentFailed        → Send "Payment Failed" email
Msg_BookingConfirmed     → Send "Booking Confirmation" email
```

### Auto-Complete

JobWorker configuration where the job automatically completes after execution:
```java
@JobWorker(type = "email-sender", autoComplete = true)
```

If `autoComplete = false`, you must explicitly call:
```java
job.newCompleteCommand().send().join();
```

---

**Last Updated**: June 8, 2026  
**Version**: 1.0.0