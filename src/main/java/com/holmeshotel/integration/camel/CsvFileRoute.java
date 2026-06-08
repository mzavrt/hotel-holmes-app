package com.holmeshotel.integration.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV File Polling Route
 *
 * Polls the data/inbox directory for .csv files (semicolon-separated),
 * skips the header, unmarshals safely using CsvDataFormat,
 * converts to a normalized Map, and sends to the Camunda starter route.
 */
@Component
public class CsvFileRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Nastavení formátu CSV (odělovač středník, přeskočit první řádek - hlavičku)
        CsvDataFormat csvFormat = new CsvDataFormat();
        csvFormat.setDelimiter(";");
        csvFormat.setSkipHeaderRecord("true"); // Geniální věc: samo to ignoruje hlavičku!

        // Route: Poll CSV files z data/inbox, po zpracování je přesuň do .archive,
        // při chybě celého souboru je přesuň do .error
        from("file://data/inbox?include=.*\\.csv$&delay=5000&move=.archive&moveFailed=.error")
            .routeId("csvFilePollingRoute")
            .log("📥 [CSV Gateway] Zpracovávám CSV soubor: ${header.CamelFileName}")
            
            // 1. Bezpečné rozebrání CSV (vyřeší i středníky v uvozovkách textu)
            .unmarshal(csvFormat)
            
            // 2. EIP: Splitter - teď máme List řádků (kde každý řádek je List Stringů), rozsekáme ho
            .split(body())
            
            // 3. EIP: Message Transformer - převod CSV řádku (Listu) na Mapu
            .process(exchange -> {
                @SuppressWarnings("unchecked")
                List<String> values = exchange.getIn().getBody(List.class);
                
                Map<String, Object> bookingData = new HashMap<>();
                
                try {
                    // Mapujeme bezpečně jen ty indexy, které existují
                    if (values.size() > 0 && !values.get(0).isEmpty()) bookingData.put("guestName", values.get(0).trim());
                    if (values.size() > 1 && !values.get(1).isEmpty()) bookingData.put("email", values.get(1).trim());
                    if (values.size() > 2 && !values.get(2).isEmpty()) bookingData.put("phoneNumber", values.get(2).trim());
                    if (values.size() > 3 && !values.get(3).isEmpty()) bookingData.put("roomType", values.get(3).trim());
                    if (values.size() > 4 && !values.get(4).isEmpty()) bookingData.put("checkInDate", values.get(4).trim());
                    if (values.size() > 5 && !values.get(5).isEmpty()) bookingData.put("checkOutDate", values.get(5).trim());
                    if (values.size() > 6 && !values.get(6).isEmpty()) bookingData.put("cardNumber", values.get(6).trim());
                    if (values.size() > 7 && !values.get(7).isEmpty()) bookingData.put("cardExpiry", values.get(7).trim());
                    if (values.size() > 8 && !values.get(8).isEmpty()) bookingData.put("guestPreferences", values.get(8).trim());
                    
                    // Add source tracking
                    bookingData.put("source", "CSV File Gateway");
                    bookingData.put("sourceFile", exchange.getIn().getHeader("CamelFileName"));
                    
                    // Nastavíme Mapu jako nové tělo zprávy
                    exchange.getIn().setBody(bookingData);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Chyba parsování CSV dat: " + values, e);
                }
            })
            
            .log("✅ [CSV Gateway] Zpracován záznam pro hosta: ${body[guestName]}")
            
            // 4. EIP: Recipient List - pošleme to Camundě
            .to("direct:startCamunda")
            
            .end();
    }
}