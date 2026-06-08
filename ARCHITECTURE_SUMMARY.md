# Apache Camel Omnichannel Gateway - Architecture Summary

## Executive Summary

This refactoring replaces your monolithic Spring `@RestController` webhook handler with an **Apache Camel-based Enterprise Service Bus (ESB)** that provides:

- ✅ **Three independent input channels** (JSON REST, XML REST, CSV File)
- ✅ **Normalized data model** across all channels
- ✅ **Loose coupling** between channels and Camunda integration
- ✅ **Enterprise-grade patterns** (EIP) for scalable integration
- ✅ **Auditability** with source tracking and correlation keys
- ✅ **Maintainability** through modular route definitions

## What Changed

### Before: Monolithic Controller
```java
@RestController
@RequestMapping("/api/webhook")
public class WebhookController {
    @PostMapping("/booking")
    public Map<String, Object> receiveExternalBooking(@RequestBody Map payload) {
        // Mix of HTTP handling, data transformation, and Camunda integration
        // Only JSON support
        // No reusability across channels
    }
}
```

### After: Camel ESB with Omnichannel Routes
```
JsonWebhookRoute         → direct:startCamunda
XmlWebhookRoute          → direct:startCamunda  → Camunda
CsvFileRoute             → direct:startCamunda
```

## New Endpoints

| Channel | Endpoint | Method | Content-Type | Processing |
|---------|----------|--------|--------------|-----------|
| JSON | `/api/camel/webhook/json` | POST | application/json | Key transformation |
| XML | `/api/camel/webhook/xml` | POST | application/xml | XPath extraction + bean |
| CSV | `data/inbox/*.csv` | File Poll | text/csv | Line splitting + parsing |

## Maven Dependencies Added

```xml
<!-- Camel REST Support -->
camel-rest-starter
camel-servlet-starter

<!-- Camel Data Marshalling -->
camel-jackson-starter       (JSON)
camel-jaxb-starter          (XML)
camel-csv-starter           (CSV)

<!-- Camel File Handling -->
camel-file-starter

<!-- Already Had -->
camel-spring-boot-starter
camunda-spring-boot-starter
```

## Project Files Created

### Camel Routes (RouteBuilder classes)
```
src/main/java/com/holmeshotel/integration/camel/
├── JsonWebhookRoute.java          (JSON REST endpoint)
├── XmlWebhookRoute.java           (XML REST endpoint + XPath)
├── CsvFileRoute.java              (CSV file polling)
├── CamundaStarterRoute.java       (Central Camunda integration)
└── XPathDataExtractor.java        (XPath extraction bean)
```

### Configuration Files
```
src/main/resources/
├── application.yml                (New: Camel + Spring config)
└── application.properties          (Existing: can be kept or deprecated)
```

### Documentation & Samples
```
├── CAMEL_INTEGRATION_GUIDE.md     (Comprehensive architecture guide)
├── CAMEL_TESTING_GUIDE.md         (Step-by-step testing instructions)
├── ARCHITECTURE_SUMMARY.md        (This file)
└── data/
    ├── inbox/                     (CSV polling directory)
    │   └── sample_bookings.csv   (Example CSV batch file)
    ├── error/                     (Failed CSV files moved here)
    └── sample_booking.xml         (Example XML payload)
```

## Key Transformations

### Channel Normalization

All three channels transform their input to this canonical format:

```json
{
  "guestName": "...",
  "email": "...",
  "phoneNumber": "...",
  "roomType": "deluxe",              // Normalized: requestedRoomType
  "checkInDate": "...",
  "checkOutDate": "...",
  "cardNumber": "...",
  "cardExpiry": "...",
  "guestPreferences": "...",         // Normalized: amenitiesDetails
  "source": "JSON API Gateway",      // Tracking: which channel
  "sourceFile": "..."                // (CSV only) which file
}
```

### Transformation Logic

| Field | JSON Path | XML XPath | CSV Index |
|-------|-----------|-----------|-----------|
| `guestName` | `guestName` | `//booking/guestName` | 0 |
| `roomType` | `requestedRoomType` ➜ transform | `//booking/requestedRoomType` ➜ extract | 3 |
| `guestPreferences` | `amenitiesDetails` ➜ transform | `//booking/amenitiesDetails` ➜ extract | 8 |

## Enterprise Integration Patterns Used

| Pattern | Component | Purpose |
|---------|-----------|---------|
| **Polling Consumer** | CsvFileRoute | Periodically poll `data/inbox` for CSV files |
| **Splitter** | CsvFileRoute | Split CSV into individual booking records |
| **Content-Based Router** | JsonWebhookRoute, XmlWebhookRoute | Route each channel to appropriate processor |
| **Message Transformer** | JsonWebhookRoute.processor | Rename keys (requestedRoomType → roomType) |
| **Content Enricher** | XmlWebhookRoute + XPathDataExtractor | Extract & normalize with XPath bean |
| **Aggregator** | CamundaStarterRoute | Convergence point: all channels merge here |
| **Service Activator** | CamundaStarterRoute.processor | Call CamundaClient to publish message |
| **Error Handler** | CsvFileRoute errorHandler | Move failed files to `data/error` |
| **Correlation** | CamundaStarterRoute | Generate & track BOOKING-<UUID> keys |

## Configuration Changes

### Spring Boot Properties → YAML

**Deprecated** `application.properties`:
```properties
# Specific Camunda settings remain same
```

**New** `application.yml` adds Camel configuration:
```yaml
camel:
  springboot:
    auto-startup: true
    context-name: holmesHotelCamelContext
  servlet:
    mapping:
      context-path: /camel
      url-pattern: /api/*
  rest:
    api-docs:
      enabled: true
      title: Holmes Hotel Camel API
```

## How It Works - End-to-End Example

### Scenario: Guest books via JSON, XML, and CSV simultaneously

```
1. JSON Request arrives at POST /api/camel/webhook/json
   │
   ├─→ [JsonWebhookRoute]
   │   ├─ Unmarshal: JSON string → Map
   │   ├─ Transform: requestedRoomType → roomType
   │   ├─ Transform: amenitiesDetails → guestPreferences
   │   └─ Add source: "JSON API Gateway"
   │
   └─→ direct:startCamunda

2. XML Request arrives at POST /api/camel/webhook/xml
   │
   ├─→ [XmlWebhookRoute]
   │   ├─ Unmarshal: XML → DOM Document
   │   ├─ Call bean: XPathDataExtractor.extractBookingData()
   │   │   └─ XPath: Extract all fields (already normalized)
   │   └─ Add source: "XML API Gateway"
   │
   └─→ direct:startCamunda

3. CSV File: data/inbox/bookings.csv
   │
   ├─→ [CsvFileRoute] Polling consumer (every 5s)
   │   ├─ Detect: bookings.csv
   │   ├─ Split: 5 lines → 5 records
   │   ├─ For each line:
   │   │   ├─ Parse: semicolon-separated values
   │   │   ├─ Create Map (already normalized keys)
   │   │   └─ Add source: "CSV File Gateway"
   │   │
   │   └─→ direct:startCamunda (×5 messages)
   │
   └─→ File processed (moved to archive)

4. Convergence Point: direct:startCamunda
   │
   ├─→ [CamundaStarterRoute] (processes all 7 messages)
   │   ├─ Generate correlation key: BOOKING-<UUID>
   │   ├─ Inject CamundaClient from Spring
   │   ├─ Call: camundaClient.newPublishMessageCommand()
   │   │   ├─ messageName: "Msg_NewBooking"
   │   │   ├─ correlationKey: "BOOKING-<UUID>"
   │   │   └─ variables: normalized Map
   │   │
   │   └─→ Camunda: 7 new process instances created

5. Response to Caller:
   {
     "status": "Accepted",
     "correlationKey": "BOOKING-<UUID>",
     "source": "JSON|XML|CSV API Gateway"
   }
```

## Advantages Over Previous Monolithic Approach

| Aspect | Before | After |
|--------|--------|-------|
| **Channels** | JSON only | JSON, XML, CSV |
| **Coupling** | Tight (HTTP → Business Logic → Camunda) | Loose (modular routes) |
| **Scalability** | Hardcoded for one format | Generic pattern reusable |
| **Testing** | Single test class | Independent route tests |
| **Maintenance** | Changes affect one controller | Isolated route changes |
| **Monitoring** | Basic logging | Rich Camel/EIP diagnostics |
| **Error Handling** | Try-catch blocks | Camel error handlers |
| **Auditability** | No source tracking | Source tracking for all |
| **Extension** | Must modify controller | Add new route class |

## Integration with Existing System

### Camunda Configuration (No Changes Needed)
Your existing Camunda credentials in `application.yml` remain:
```yaml
camunda:
  client:
    mode: saas
    auth:
      client-id: <your-id>
      client-secret: <your-secret>
    cloud:
      cluster-id: <your-cluster>
      region: fra-1
```

### BPMN Process Requirements
Ensure your Camunda BPMN diagram has:
- ✅ **Message Start Event** with `messageName: "Msg_NewBooking"`
- ✅ Process variables to receive: `roomType`, `guestPreferences`, `source`, etc.

### Database (No Changes)
Your H2 database configuration remains unchanged. Entity classes and repositories work as before.

## Migration Path

If you want to gradually migrate:

1. **Keep the old controller** for backward compatibility
2. **Add new Camel routes** alongside it
3. **Update external clients** to use new endpoints (`/api/camel/webhook/*`)
4. **Monitor Camel routes** to ensure correctness
5. **Deprecate old controller** once confidence is high

## Performance Characteristics

| Metric | Value |
|--------|-------|
| JSON Parsing | Milliseconds (Jackson) |
| XML XPath Extraction | Milliseconds (Dom4j via JAXB) |
| CSV Line Split | Microseconds per line |
| CSV Batch Size | No limit (streams records) |
| CSV Polling Interval | 5 seconds (configurable) |
| Camunda Message Send | Async (non-blocking) |
| Concurrent Channels | All supported simultaneously |

## Security Considerations

✅ **HTTPS** - Incoming REST requests should use TLS  
✅ **Authentication** - Add Spring Security filters if needed  
✅ **Card Data** - PII data passed through to Camunda variables (review encryption)  
✅ **File Access** - CSV polling restricted to `data/inbox/` directory  
✅ **Error Messages** - Don't expose stack traces to clients (handled by error route)  

## Monitoring & Operations

### Health Check
```bash
GET http://localhost:8080/actuator/health
```

### Camel Routes Status
```bash
GET http://localhost:8080/camel/actuator/camel/routes
```

### Active Routes
- `jsonWebhookRoute` - JSON REST endpoint
- `xmlWebhookRoute` - XML REST endpoint
- `csvFilePollingRoute` - CSV file polling
- `camundaStarterRoute` - Camunda integration
- `camundaErrorHandler` - Error handling

### Logging
Configure in `application.yml`:
```yaml
logging:
  level:
    org.apache.camel: INFO        # Camel routing
    com.holmeshotel: DEBUG        # Your code
```

## Next Steps

1. **Review** the integration guide and testing guide
2. **Build** with `mvn clean package`
3. **Test** each endpoint with provided curl commands
4. **Monitor** logs to verify transformations
5. **Validate** Camunda receives correct messages
6. **Scale** by adding more input channels using same pattern

## Support & Troubleshooting

See:
- `CAMEL_INTEGRATION_GUIDE.md` - Architecture & component details
- `CAMEL_TESTING_GUIDE.md` - Step-by-step testing procedures
- Route source code - Well-commented with EIP pattern labels

## Key Takeaway

You've upgraded from a monolithic webhook controller to a flexible, enterprise-grade **Omnichannel Integration Gateway** that:
- Accepts data from multiple sources (JSON, XML, CSV)
- Normalizes to a single canonical model
- Integrates cleanly with Camunda 8
- Follows industry-standard Enterprise Integration Patterns
- Scales horizontally with Camel's proven architecture

