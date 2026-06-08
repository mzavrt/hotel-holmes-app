package com.holmeshotel.integration.camel;

import org.apache.camel.language.xpath.XPath;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * XPath Data Extractor Bean
 * * Camel automaticky zpracuje XML tělo zprávy, vyhodnotí @XPath anotace
 * a předá výsledky přímo do této metody jako String parametry.
 */
@Component
public class XPathDataExtractor {

    public Map<String, Object> extractBookingData(
            @XPath("//booking/guestName/text()") String guestName,
            @XPath("//booking/email/text()") String email,
            @XPath("//booking/phoneNumber/text()") String phoneNumber,
            @XPath("//booking/requestedRoomType/text()") String roomType,
            @XPath("//booking/checkInDate/text()") String checkInDate,
            @XPath("//booking/checkOutDate/text()") String checkOutDate,
            @XPath("//booking/cardNumber/text()") String cardNumber,
            @XPath("//booking/cardExpiry/text()") String cardExpiry,
            @XPath("//booking/amenitiesDetails/text()") String amenities) {
        
        Map<String, Object> bookingData = new HashMap<>();
        
        // Vložení do mapy pouze pokud hodnota existuje a není prázdná
        // (XPath vrací často prázdný String "", pokud je XML tag prázdný např. <email></email>)
        putIfNotEmpty(bookingData, "guestName", guestName);
        putIfNotEmpty(bookingData, "email", email);
        putIfNotEmpty(bookingData, "phoneNumber", phoneNumber);
        putIfNotEmpty(bookingData, "roomType", roomType); // Rovnou normalizováno!
        putIfNotEmpty(bookingData, "checkInDate", checkInDate);
        putIfNotEmpty(bookingData, "checkOutDate", checkOutDate);
        putIfNotEmpty(bookingData, "cardNumber", cardNumber);
        putIfNotEmpty(bookingData, "cardExpiry", cardExpiry);
        putIfNotEmpty(bookingData, "guestPreferences", amenities); // Rovnou normalizováno!
        
        bookingData.put("source", "XML API Gateway");
        
        return bookingData;
    }

    // Pomocná metoda pro čistší kód
    private void putIfNotEmpty(Map<String, Object> map, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            map.put(key, value.trim());
        }
    }
}