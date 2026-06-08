# Migration Guide: From Spring Controller to Apache Camel

## Decision: Keep or Replace?

### Option 1: Run Both (Recommended for Gradual Migration)
- Keep existing `WebhookController` for backward compatibility
- Deploy new Camel routes alongside
- Redirect external clients to new endpoints
- Deprecate old controller after validation period

### Option 2: Full Replacement (Clean Break)
- Delete `WebhookController`
- Update all external clients to use new Camel endpoints
- Requires testing in staging before production

This document provides guidance for both approaches.

## Side-by-Side Comparison

### Old Monolithic Approach

**Code**: `src/main/java/com/holmeshotel/controller/WebhookController.java`

```java
@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    @Autowired
    private CamundaClient camundaClient;

    @PostMapping("/booking")
    public Map<String, Object> receiveExternalBooking(@RequestBody Map<String, Object> incomingPayload) {
        // 1. Prepare payload
        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("guestName", incomingPayload.get("guestName"));
        processVariables.put("email", incomingPayload.get("email"));
        // ... more fields ...
        processVariables.put("requestedRoomType", incomingPayload.get("requestedRoomType"));
        processVariables.put("amenitiesDetails", incomingPayload.get("amenitiesDetails"));
        
        // 2. Trigger Camunda
        camundaClient.newPublishMessageCommand()
            .messageName("Msg_NewBooking")
            .correlationKey("")    // Empty = all instances
            .variables(processVariables)
            .send()
            .join();
            
        // 3. Return response
        return Map.of("status", "Accepted");
    }
}
```

**Limitations**:
- ❌ Only accepts JSON
- ❌ No XML support
- ❌ No CSV batch support
- ❌ No key normalization/transformation
- ❌ No source tracking
- ❌ Correlation key always empty

---

### New Camel Approach

**Code**: Multiple modular routes
- `JsonWebhookRoute` - JSON REST
- `XmlWebhookRoute` - XML REST  
- `CsvFileRoute` - CSV polling
- `CamundaStarterRoute` - Central integration
- `XPathDataExtractor` - XML data extraction

**Advantages**:
- ✅ Supports 3 input channels
- ✅ Key normalization (requestedRoomType → roomType)
- ✅ Source tracking
- ✅ Unique correlation keys
- ✅ Modular and testable
- ✅ Enterprise patterns (EIP)
- ✅ Scalable to additional channels

---

## Endpoint Mapping

### Old Endpoints (Deprecate)
```
POST /api/webhook/booking         → JSON only
POST /api/webhook/service-order   → JSON only
POST /api/webhook/complaint       → JSON only
```

### New Endpoints (Promote)
```
POST /api/camel/webhook/json      → JSON (replaces /api/webhook/booking)
POST /api/camel/webhook/xml       → XML (new capability)
File: data/inbox/*.csv             → CSV polling (new capability)
```

---

## Client Migration Checklist

### Step 1: Test New Endpoints (Parallel)
- [ ] Test JSON endpoint with sample booking
- [ ] Test XML endpoint with sample XML
- [ ] Verify Camunda receives messages
- [ ] Confirm key transformations occur

### Step 2: Deploy New Routes (Parallel)
- [ ] Deploy new Camel route classes
- [ ] Update `pom.xml` with new dependencies
- [ ] Update `application.yml` with Camel config
- [ ] Run integration tests

### Step 3: Verify Data Integrity
- [ ] Compare old vs new Camunda variables
- [ ] Verify key names match (roomType, guestPreferences)
- [ ] Check source tracking in Camunda audit logs
- [ ] Validate business process execution

### Step 4: Redirect External Clients (Phased)
- [ ] Update 10% of clients to new endpoints
- [ ] Monitor logs for 1-2 weeks
- [ ] Update 50% of clients
- [ ] Monitor for another week
- [ ] Update remaining 40%

### Step 5: Deprecate Old Controller (Optional)
- [ ] Set endpoint deprecation headers
- [ ] Document cutoff date
- [ ] Delete after grace period

---

## Backward Compatibility Layer (Optional)

If you want to support both old and new formats during migration:

```java
@Component
public class LegacyWebhookRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        // Support old endpoint format while redirecting to new Camel route
        rest()
            .post("/webhook/booking")
            .to("direct:normalizeJsonPayload");  // Reuse JSON normalization
    }
}
```

This allows old clients to keep using `/api/webhook/booking` while internally routing through Camel.

---

## Key Transformation Validation

### Verify Transformation Occurred

**Before (Old Approach)**:
```json
// Sent to Camunda as-is
{
  "guestName": "John",
  "requestedRoomType": "deluxe",
  "amenitiesDetails": "non-smoking"
}
```

**After (New Approach)**:
```json
// Transformed before sending to Camunda
{
  "guestName": "John",
  "roomType": "deluxe",              // ← Renamed
  "guestPreferences": "non-smoking", // ← Renamed
  "source": "JSON API Gateway"       // ← Added
}
```

**How to Verify in Camunda**:
1. Open Camunda Console
2. Find recent process instance
3. Check variables tab
4. Look for:
   - ✅ `roomType` (not `requestedRoomType`)
   - ✅ `guestPreferences` (not `amenitiesDetails`)
   - ✅ `source` field present

---

## Load Testing - Old vs New

### Old Approach Load Test
```bash
# JSON only, no batch processing
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/webhook/booking \
    -H "Content-Type: application/json" \
    -d '{"guestName":"Guest'$i'", ...}' &
done
```

### New Approach Load Test
```bash
# Option 1: JSON requests (same as old, but now normalized)
for i in {1..100}; do
  curl -X POST http://localhost:8080/camel/api/webhook/json \
    -H "Content-Type: application/json" \
    -d '{"guestName":"Guest'$i'", ...}' &
done

# Option 2: XML requests (new capability)
for i in {1..100}; do
  curl -X POST http://localhost:8080/camel/api/webhook/xml \
    -H "Content-Type: application/xml" \
    -d '<booking><guestName>Guest'$i'</guestName>...</booking>' &
done

# Option 3: CSV batch (100 bookings in one file)
echo "guestName;...
Guest1;...
Guest2;...
...
Guest100;..." > data/inbox/batch_100.csv
# Camel processes all 100 in ~5 seconds
```

---

## Rollback Plan

If issues occur after deployment:

### Immediate Rollback
1. Stop the application
2. Remove new Camel route classes from classpath
3. Remove Camel dependencies from `pom.xml`
4. Revert `application.yml` to previous state
5. Restart application - reverts to old `WebhookController`

### Graceful Rollback
If both are running:
1. Update DNS/load balancer to route to old endpoint
2. Stop processing on Camel routes
3. Clients automatically fall back to old controller
4. Monitor until stable

---

## Monitoring Old vs New

### Metrics to Compare

| Metric | Old Controller | New Camel | Expected |
|--------|----------------|-----------|----------|
| Requests/sec | Baseline | >= Baseline | Similar or better |
| Response time | ms | ms | Within 50ms |
| Error rate | % | % | <= % old |
| Camunda messages | Count | Count | Same count |
| CPU usage | % | % | Similar |
| Memory usage | MB | MB | ± 50MB |

### Log Search Queries

**Old approach**:
```
grep "receiveExternalBooking" logs.txt
```

**New approach**:
```
grep "\[JSON Gateway\]|\[XML Gateway\]|\[CSV Gateway\]" logs.txt
```

---

## External API Client Updates

### Old Python Client
```python
import requests

payload = {
    "guestName": "John",
    "requestedRoomType": "deluxe",  # Old key name
    "amenitiesDetails": "non-smoking"
}

response = requests.post(
    "http://localhost:8080/api/webhook/booking",  # Old endpoint
    json=payload
)
```

### New Python Client (Option 1: Minimal Change)
```python
import requests

payload = {
    "guestName": "John",
    "requestedRoomType": "deluxe",  # Still works (Camel transforms)
    "amenitiesDetails": "non-smoking"
}

response = requests.post(
    "http://localhost:8080/camel/api/webhook/json",  # New endpoint
    json=payload
)
```

### New Python Client (Option 2: Leverage New Features - XML)
```python
import requests
import xml.etree.ElementTree as ET

root = ET.Element("booking")
ET.SubElement(root, "guestName").text = "John"
ET.SubElement(root, "requestedRoomType").text = "deluxe"
ET.SubElement(root, "amenitiesDetails").text = "non-smoking"

xml_string = ET.tostring(root, encoding='unicode')

response = requests.post(
    "http://localhost:8080/camel/api/webhook/xml",  # New XML endpoint
    data=xml_string,
    headers={"Content-Type": "application/xml"}
)
```

### New Integration: CSV Batch (New Feature)
```python
import csv

# Create CSV file with 1000 bookings
with open("bookings.csv", "w", newline="") as f:
    writer = csv.writer(f, delimiter=";")
    writer.writerow(["guestName", "email", "phoneNumber", ...])  # Header
    for i in range(1000):
        writer.writerow([f"Guest{i}", f"guest{i}@test.com", ...])

# Copy to polling directory
import shutil
shutil.copy("bookings.csv", "data/inbox/bookings_1000.csv")

# Camel automatically polls and processes all 1000 records
# Check Camunda for 1000 new process instances
```

---

## FAQ: Migration

### Q: Do I have to migrate immediately?
**A**: No. Both can run in parallel. Migrate when ready.

### Q: Will old clients break?
**A**: No, if you keep `WebhookController`. New routes are added alongside.

### Q: How do I test without affecting production?
**A**: Deploy to staging first, test new routes, then roll out to production.

### Q: Can I migrate one channel at a time?
**A**: Yes. Start with JSON (most common), then XML, then CSV.

### Q: What if I find a bug in Camel transformation?
**A**: Fix the route, redeploy, no need to touch business logic or Camunda.

### Q: How do I handle the `correlationKey` change?
**A**: Old: empty string (all instances). New: unique BOOKING-<UUID>. 
This is actually an improvement for message correlation.

### Q: Do my Camunda BPMN diagrams need changes?
**A**: No, they remain the same. The process receives the same message `Msg_NewBooking`.

### Q: Can I use Camel with my existing entities and repositories?
**A**: Yes, routes only handle message ingestion. Repository layer unchanged.

---

## Deployment Checklist

### Pre-Deployment
- [ ] Merge Camel code into main branch
- [ ] Update `pom.xml` (new dependencies)
- [ ] Update `application.yml` (Camel config)
- [ ] Run unit tests for each route
- [ ] Run integration tests with Camunda SaaS
- [ ] Load test on staging (100+ requests/sec)

### Deployment
- [ ] Tag release version
- [ ] Build Docker image (if containerized)
- [ ] Deploy to staging
- [ ] Smoke test all 3 endpoints
- [ ] Monitor logs for 2 hours
- [ ] Deploy to production (blue-green or canary)

### Post-Deployment
- [ ] Monitor Camel routes in production
- [ ] Verify Camunda receives messages
- [ ] Check correlation keys in audit logs
- [ ] Gradually shift clients to new endpoints
- [ ] Deprecate old endpoints after 1-2 weeks

---

## Support Resources

- `CAMEL_INTEGRATION_GUIDE.md` - Architecture details
- `CAMEL_TESTING_GUIDE.md` - Testing procedures
- `ARCHITECTURE_SUMMARY.md` - High-level overview
- Route source code - Well-commented with EIP patterns
- Apache Camel docs: https://camel.apache.org/

