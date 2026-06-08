# Apache Camel Omnichannel Gateway for Holmes Hotel

## Architecture Overview

This integration implements an **Enterprise Service Bus (ESB)** pattern using Apache Camel to consolidate multiple input channels (JSON REST, XML REST, CSV Files) into a single normalized format before triggering Camunda 8 business processes.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Holmes Hotel Camel Gateway                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────┐   ┌──────────────────┐   ┌─────────────┐  │
│  │  JSON REST API   │   │   XML REST API   │   │  CSV Files  │  │
│  │ /webhook/json    │   │  /webhook/xml    │   │ data/inbox  │  │
│  └────────┬─────────┘   └────────┬─────────┘   └─────┬───────┘  │
│           │                      │                    │          │
│           ▼                      ▼                    ▼          │
│  ┌──────────────────┐   ┌──────────────────┐   ┌─────────────┐  │
│  │   Normalize      │   │  Extract via     │   │  Parse CSV  │  │
│  │   JSON Keys      │   │  XPath + Bean    │   │  to Map     │  │
│  └────────┬─────────┘   └────────┬─────────┘   └─────┬───────┘  │
│           │                      │                    │          │
│           └──────────────────────┼────────────────────┘          │
│                                  ▼                               │
│                  ┌──────────────────────────┐                    │
│                  │ Aggregation Point        │                    │
│                  │ direct:startCamunda      │                    │
│                  └────────────┬─────────────┘                    │
│                               ▼                                  │
│                  ┌──────────────────────────┐                    │
│                  │ Publish to Camunda       │                    │
│                  │ messageName: Msg_NewB...│                    │
│                  └─────────────────────────┘                    │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Components

### 1. JsonWebhookRoute
**Endpoint**: `POST /api/camel/webhook/json`  
**Input**: JSON with fields like `requestedRoomType`, `amenitiesDetails`  
**Processing**:
- Unmarshal JSON to Map
- **Transform**: `requestedRoomType` → `roomType`
- **Transform**: `amenitiesDetails` → `guestPreferences`
- Add source tracking: `"source": "JSON API Gateway"`

**Example Request**:
```bash
curl -X POST http://localhost:8080/camel/api/webhook/json \
  -H "Content-Type: application/json" \
  -d '{
    "guestName": "John Doe",
    "email": "john@example.com",
    "phoneNumber": "+1234567890",
    "requestedRoomType": "deluxe",
    "checkInDate": "2024-06-15",
    "checkOutDate": "2024-06-18",
    "cardNumber": "4532123456789",
    "cardExpiry": "12/25",
    "amenitiesDetails": "Extra pillow, non-smoking"
  }'
```

### 2. XmlWebhookRoute
**Endpoint**: `POST /api/camel/webhook/xml`  
**Input**: XML with booking data structure  
**Processing**:
- Unmarshal XML to DOM Document
- Extract data using **XPath expressions** (via `XPathDataExtractor` bean)
- Keys are already normalized during extraction
- Add source tracking: `"source": "XML API Gateway"`

**Example XML Payload**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<booking>
    <guestName>Jane Smith</guestName>
    <email>jane@example.com</email>
    <phoneNumber>+9876543210</phoneNumber>
    <requestedRoomType>suite</requestedRoomType>
    <checkInDate>2024-06-20</checkInDate>
    <checkOutDate>2024-06-23</checkOutDate>
    <cardNumber>5412345678901234</cardNumber>
    <cardExpiry>06/26</cardExpiry>
    <amenitiesDetails>Late checkout, airport transfer</amenitiesDetails>
</booking>
```

**Example Request**:
```bash
curl -X POST http://localhost:8080/camel/api/webhook/xml \
  -H "Content-Type: application/xml" \
  -d @booking.xml
```

### 3. CsvFileRoute
**Polling Directory**: `data/inbox/`  
**File Pattern**: `*.csv`  
**Format**: Semicolon-separated values  
**Processing**:
- Polls directory every 5 seconds
- Splits file into individual records (lines)
- Parses each CSV line into a Map
- Keys already in normalized form (e.g., `roomType`, `guestPreferences`)
- Add source tracking: `"source": "CSV File Gateway"` + `"sourceFile": "<filename>"`

**Expected CSV Header & Format**:
```
guestName;email;phoneNumber;requestedRoomType;checkInDate;checkOutDate;cardNumber;cardExpiry;amenitiesDetails
John Doe;john@hotel.com;+1111111111;deluxe;2024-07-01;2024-07-03;4111111111111111;07/25;Extra towels
Jane Smith;jane@hotel.com;+2222222222;standard;2024-07-05;2024-07-10;5111111111111111;08/26;Non-smoking
```

**CSV File Example**: `data/inbox/bookings_20240608.csv`

### 4. CamundaStarterRoute
**Route**: `direct:startCamunda` (internal Camel route)  
**Purpose**: Central aggregation point for all channels  
**Processing**:
- Receives normalized Map from any channel
- Injects `CamundaClient` from Spring context
- Generates unique correlation key: `BOOKING-<UUID>`
- Publishes Camunda message: `Msg_NewBooking`
- Returns JSON acknowledgment with status and correlation key

**Response Format**:
```json
{
  "status": "Accepted",
  "message": "Booking request received and injected into process engine",
  "correlationKey": "BOOKING-550e8400-e29b-41d4-a716-446655440000",
  "processMessage": "Msg_NewBooking",
  "guestName": "John Doe",
  "source": "JSON API Gateway"
}
```

## Enterprise Integration Patterns (EIP)

| Route | Pattern | Description |
|-------|---------|-------------|
| JsonWebhookRoute | Content-Based Router | Routes JSON to transformation step |
| JsonWebhookRoute | Message Transformer | Renames keys to normalize data |
| XmlWebhookRoute | Content Enricher | Extracts data via XPath bean |
| CsvFileRoute | Polling Consumer | Periodically polls for .csv files |
| CsvFileRoute | Splitter | Splits CSV into individual records |
| CsvFileRoute | Batch Processing | Processes multiple bookings per file |
| CamundaStarterRoute | Aggregator | Convergence point for all channels |
| CamundaStarterRoute | Service Activator | Calls external Camunda service |

## Key Data Transformations

### Incoming Key Names → Normalized Names

| Source | Incoming Key | Normalized Key | Purpose |
|--------|--------------|----------------|---------|
| JSON | `requestedRoomType` | `roomType` | Shorter, consistent naming |
| JSON | `amenitiesDetails` | `guestPreferences` | Business-friendly naming |
| XML | `requestedRoomType` | `roomType` | XPath extraction normalizes |
| XML | `amenitiesDetails` | `guestPreferences` | XPath extraction normalizes |
| CSV | Already normalized | `roomType` | CSV structure matches normalized form |
| CSV | Already normalized | `guestPreferences` | CSV structure matches normalized form |
| All | N/A | `source` | Tracking: "JSON API Gateway", "XML API Gateway", "CSV File Gateway" |
| CSV | N/A | `sourceFile` | File name for audit trail |

## File Structure

```
src/main/java/com/holmeshotel/integration/camel/
├── CamundaStarterRoute.java      # Central hub (all channels converge here)
├── JsonWebhookRoute.java         # JSON REST endpoint
├── XmlWebhookRoute.java          # XML REST endpoint
├── CsvFileRoute.java             # CSV file polling
└── XPathDataExtractor.java       # XPath extraction bean

src/main/resources/
├── application.yml               # Camel + Spring configuration
├── application.properties         # Keep for backward compatibility
└── data/
    ├── inbox/                    # CSV files polled by CsvFileRoute
    └── error/                    # Failed files moved here by error handler

```

## Configuration (application.yml)

```yaml
camel:
  springboot:
    auto-startup: true
    context-name: holmesHotelCamelContext
    jmx-enabled: true
  servlet:
    mapping:
      context-path: /camel
      url-pattern: /api/*
  rest:
    api-docs:
      enabled: true
      title: Holmes Hotel Camel API
```

## Camunda Integration

All routes eventually publish a Camunda message with:

| Field | Value |
|-------|-------|
| `messageName` | `Msg_NewBooking` |
| `correlationKey` | `BOOKING-<UUID>` |
| `variables` | Normalized Map with guest & booking data |

**Camunda Diagram Requirement**: Your BPMN process must have a **Message Start Event** configured with:
- **Message Name**: `Msg_NewBooking`
- **Correlation Key Variable**: Any variable for tracking (optional)

## Error Handling

### CSV File Errors
- Failed files are moved to `data/error/`
- Route continues processing other files
- Logs include detailed error messages

### Camunda Communication Errors
- Exception caught and logged
- Returns error response:
  ```json
  {
    "status": "Error",
    "message": "<exception message>"
  }
  ```

### XPath Extraction Errors
- XML parsing failures logged
- Route fails gracefully
- HTTP 500 returned to caller

## Testing

### Test JSON Endpoint
```bash
curl -X POST http://localhost:8080/camel/api/webhook/json \
  -H "Content-Type: application/json" \
  -d '{"guestName":"Test","email":"test@test.com","requestedRoomType":"deluxe"}'
```

### Test XML Endpoint
```bash
curl -X POST http://localhost:8080/camel/api/webhook/xml \
  -H "Content-Type: application/xml" \
  -d '<booking><guestName>Test</guestName><requestedRoomType>deluxe</requestedRoomType></booking>'
```

### Test CSV File Processing
1. Create `data/inbox/test.csv` with sample records
2. Wait 5 seconds for polling cycle
3. Check Camunda for new process instances
4. File auto-moves to processed location or error location

### Monitor Routes
- Camel Management Console: `http://localhost:8080/camel`
- Routes logged with routeId: `jsonWebhookRoute`, `xmlWebhookRoute`, `csvFilePollingRoute`, `camundaStarterRoute`

## Advantages of This Architecture

✅ **Omnichannel Support**: Same business logic for JSON, XML, CSV  
✅ **Loose Coupling**: Routes are independent, easy to modify  
✅ **Scalability**: Camel handles concurrent requests efficiently  
✅ **Maintainability**: Clear separation of concerns  
✅ **Auditability**: Source tracking for all bookings  
✅ **Resilience**: Error handling and retry strategies  
✅ **Monitoring**: Detailed logging at each transformation step  

## Next Steps

1. **Deploy** this configuration and test each endpoint
2. **Monitor** logs to verify data transformations
3. **Create CSV files** in `data/inbox/` to test batch processing
4. **Validate Camunda** receives messages with correct correlation keys
5. **Scale** by adding more channels (SOAP, SFTP, EDI) to the same pattern

