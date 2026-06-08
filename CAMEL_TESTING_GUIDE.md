# Quick Start Testing Guide - Apache Camel Omnichannel Gateway

## Setup

### 1. Build the Project
```bash
mvn clean package
```

### 2. Start the Application
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Testing Each Channel

### Channel 1: JSON REST API

**Test 1 - Basic JSON Booking**
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
    "cardNumber": "4532123456789012",
    "cardExpiry": "12/25",
    "amenitiesDetails": "Non-smoking, extra pillow"
  }'
```

**Expected Response**:
```json
{
  "status": "Accepted",
  "message": "Booking request received and injected into process engine",
  "correlationKey": "BOOKING-<UUID>",
  "processMessage": "Msg_NewBooking",
  "guestName": "John Doe",
  "source": "JSON API Gateway"
}
```

**Verify Key Transformations**:
- ✅ `requestedRoomType` was renamed to `roomType` in Camunda variables
- ✅ `amenitiesDetails` was renamed to `guestPreferences` in Camunda variables
- ✅ `source` field set to "JSON API Gateway"

---

### Channel 2: XML REST API

**Test 2 - Basic XML Booking**
```bash
curl -X POST http://localhost:8080/camel/api/webhook/xml \
  -H "Content-Type: application/xml" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<booking>
    <guestName>Jane Smith</guestName>
    <email>jane@example.com</email>
    <phoneNumber>+1-555-0456</phoneNumber>
    <requestedRoomType>suite</requestedRoomType>
    <checkInDate>2024-06-20</checkInDate>
    <checkOutDate>2024-06-23</checkOutDate>
    <cardNumber>5412345678901234</cardNumber>
    <cardExpiry>06/26</cardExpiry>
    <amenitiesDetails>Late checkout; airport transfer</amenitiesDetails>
</booking>'
```

**Expected Response**:
```json
{
  "status": "Accepted",
  "message": "Booking request received and injected into process engine",
  "correlationKey": "BOOKING-<UUID>",
  "processMessage": "Msg_NewBooking",
  "guestName": "Jane Smith",
  "source": "XML API Gateway"
}
```

**Verify XPath Extraction**:
- ✅ XPath expressions extracted all booking fields from XML
- ✅ `requestedRoomType` already normalized to `roomType` during XPath extraction
- ✅ `amenitiesDetails` already normalized to `guestPreferences` during extraction
- ✅ `source` field set to "XML API Gateway"

---

### Channel 3: CSV File Polling

**Test 3 - Process CSV Batch File**

1. **Copy the sample CSV file** (or create your own):
   ```bash
   cp data/inbox/sample_bookings.csv data/inbox/bookings_$(date +%s).csv
   ```

2. **Monitor the logs** for file polling and processing:
   ```
   [INFO] 📥 [CSV Gateway] Processing CSV file: bookings_1718367890.csv
   [INFO] ✅ [CSV Gateway] Parsed booking record from file: bookings_1718367890.csv
   [INFO] 🔄 [Camunda Gateway] Normalizing booking data from source: CSV File Gateway
   [INFO] ✅ [Camunda Gateway] Response prepared
   ```

3. **Check Camunda** for 5 new process instances (one per CSV line):
   - Each should have unique correlation key: `BOOKING-<UUID>`
   - Each should have `source: "CSV File Gateway"`
   - Each should have `sourceFile: "bookings_1718367890.csv"`

**CSV Format Reminder**:
```
guestName;email;phoneNumber;requestedRoomType;checkInDate;checkOutDate;cardNumber;cardExpiry;amenitiesDetails
John Doe;john@email.com;+1111111111;deluxe;2024-06-15;2024-06-18;4111111111111111;12/25;Extra pillow
```

---

## Monitoring & Verification

### 1. Check Application Logs
```bash
tail -f application.log | grep -E "\[JSON|XML|CSV|Camunda\]"
```

### 2. Verify Data Transformation
Look for these log messages:
- JSON: `✅ [JSON Gateway] Payload normalized. Keys transformed successfully`
- XML: `✅ [XML Gateway] Data extracted via XPath. Keys normalized successfully`
- CSV: `✅ [CSV Gateway] Parsed booking record from file`

### 3. Verify Camunda Integration
Look for:
```
📤 [Camunda] Triggering Camunda process for guest: <name>
   Source: <JSON|XML|CSV> API Gateway
✅ [Camunda] Successfully published message to Camunda
```

### 4. Check Camunda SaaS Console
- Go to https://console.cloud.camunda.io (or your Camunda instance)
- Look for new process instances with correlation key starting with `BOOKING-`
- Verify process variables include:
  - ✅ `guestName`
  - ✅ `roomType` (not `requestedRoomType`)
  - ✅ `guestPreferences` (not `amenitiesDetails`)
  - ✅ `source` (tracking origin)

---

## Troubleshooting

### Problem: No process instances in Camunda
**Solution**:
1. Verify Camunda client credentials in `application.yml`
2. Check network connectivity to Camunda cluster
3. Verify `Msg_NewBooking` Message Start Event exists in BPMN diagram
4. Check logs for: `❌ [Camunda] Failed to publish message`

### Problem: CSV file not processed
**Solution**:
1. Verify file is in `data/inbox/` directory
2. Verify filename ends with `.csv`
3. Check logs for polling: `📥 [CSV Gateway] Processing CSV file`
4. Look for parsing errors in logs

### Problem: XML parsing fails
**Solution**:
1. Validate XML structure matches expected schema
2. Verify all required fields are present
3. Check XPath expressions in `XPathDataExtractor.java`
4. Look for: `❌ [XML Gateway] Failed to extract booking data from XML`

### Problem: Keys not transforming correctly
**Solution**:
- JSON: Check `JsonWebhookRoute` processor for key mapping logic
- XML: Check `XPathDataExtractor.extractBookingData()` method
- CSV: Check `CsvFileRoute` for column mapping (index 3 = roomType, etc.)

---

## Advanced Testing

### Load Testing - Send Multiple Requests
```bash
for i in {1..10}; do
  curl -X POST http://localhost:8080/camel/api/webhook/json \
    -H "Content-Type: application/json" \
    -d "{\"guestName\":\"Guest $i\",\"email\":\"guest$i@test.com\",\"requestedRoomType\":\"deluxe\"}" &
done
wait
```

### Test Error Handling
```bash
# Send invalid JSON
curl -X POST http://localhost:8080/camel/api/webhook/json \
  -H "Content-Type: application/json" \
  -d 'invalid json'

# Send invalid XML
curl -X POST http://localhost:8080/camel/api/webhook/xml \
  -H "Content-Type: application/xml" \
  -d '<invalid>unclosed'
```

### Monitor Route Metrics
Access Camel Management Console:
```
http://localhost:8080/camel/
```

---

## Expected Log Output

### JSON Request Successful Path:
```
📥 [JSON Gateway] Received booking request from JSON endpoint
✅ [JSON Gateway] Payload normalized. Keys transformed successfully
🔄 [Camunda Gateway] Normalizing booking data from source: JSON API Gateway
📤 [Camunda] Triggering Camunda process for guest: John Doe
   Source: JSON API Gateway
✅ [Camunda] Successfully published message to Camunda
✅ [Camunda Gateway] Response prepared
```

### CSV Batch Processing Path:
```
📥 [CSV Gateway] Processing CSV file: bookings_1718367890.csv
✅ [CSV Gateway] Parsed booking record from file: bookings_1718367890.csv
🔄 [Camunda Gateway] Normalizing booking data from source: CSV File Gateway
📤 [Camunda] Triggering Camunda process for guest: John Doe
   Source: CSV File Gateway (File: bookings_1718367890.csv)
✅ [Camunda] Successfully published message to Camunda
✅ [Camunda Gateway] Response prepared
```

---

## Success Criteria

- ✅ JSON endpoint accepts requests and returns 200 OK
- ✅ XML endpoint extracts XPath data correctly
- ✅ CSV files are polled every 5 seconds
- ✅ All key transformations occur (requestedRoomType → roomType, etc.)
- ✅ Camunda receives messages with correct variables
- ✅ Correlation keys are unique and tracked
- ✅ Source information is captured for audit trails

