package com.holmeshotel.integration.camel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Message End Event Email Route
 * 
 * This route handles all email notifications triggered by message end events in the BPMN diagram.
 * It receives a Map containing:
 * - messageName: Which message end event triggered this (RESERVATION_DENIED, DETAILS_REJECTED, etc.)
 * - Process variables: guestName, email, roomType, checkInDate, checkOutDate, etc.
 * 
 * The route uses Content-Based Router EIP to select the appropriate email template,
 * then logs a beautifully formatted ASCII email box to the console (mocking the email send).
 * 
 * EIP Patterns Used:
 * - Service Activator: Receives from Zeebe JobWorker
 * - Content-Based Router: Routes based on messageName value
 * - Message Enricher: Adds Subject and Body to the exchange
 * - Logging: Pretty-prints mock email to console
 */
@Component
public class MessageEndEventEmailRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Route: Main email handler
        from("direct:mockEmailRouter")
            
            .routeId("messageEndEventEmailRoute") 
            .log("📧 [Email Route] Received email request for message: ${body[messageName]}")
            
            // EIP: Content-Based Router - Route based on the message type
            .choice()
            
                // Case 1: Reservation Denied
                .when().simple("${body[messageName]} == 'RESERVATION_DENIED'")
                    
                    .log("❌ [Email Route] RESERVATION_DENIED - Guest ${body[guestName]} will be notified")
                    .process(exchange -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> body = exchange.getIn().getBody(Map.class);
                        
                        String subject = "Reservation Update - Holmes Hotel";
                        String checkInDate = (String) body.getOrDefault("checkInDate", "N/A");
                        String bodyText = "Dear " + body.getOrDefault("guestName", "Guest") + ",\n\n" +
                            "Unfortunately, we must inform you that the room you requested is fully booked for " + checkInDate + ".\n" +
                            "We sincerely apologize for any inconvenience.\n\n" +
                            "Please visit our website to explore alternative dates or room types.\n\n" +
                            "Best regards,\n" +
                            "Holmes Hotel Team";
                        
                        body.put("subject", subject);
                        body.put("body", bodyText);
                        exchange.getIn().setBody(body);
                    })
                    .to("direct:logMockEmail")
                
                // Case 2: Details Rejected
                .when().simple("${body[messageName]} == 'DETAILS_REJECTED'")
                  
                    .log("⚠️ [Email Route] DETAILS_REJECTED - Guest ${body[guestName]} has validation issues")
                    .process(exchange -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> body = exchange.getIn().getBody(Map.class);
                        
                        String subject = "Action Required: Issue with your Holmes Hotel Booking";
                        String roomType = (String) body.getOrDefault("roomType", "your room");
                        String bodyText = "Dear " + body.getOrDefault("guestName", "Guest") + ",\n\n" +
                            "We encountered an issue with your booking details for the " + roomType + " room.\n" +
                            "Please contact our support team to resolve this matter and complete your reservation.\n\n" +
                            "Support Contact: support@holmeshotel.com | Phone: +1-800-HOLMES\n\n" +
                            "Best regards,\n" +
                            "Holmes Hotel Customer Service";
                        
                        body.put("subject", subject);
                        body.put("body", bodyText);
                        exchange.getIn().setBody(body);
                    })
                    .to("direct:logMockEmail")
                
                // Case 3: Payment Failed
                .when().simple("${body[messageName]} == 'PAYMENT_FAILED'")
                    
                    .log("💳 [Email Route] PAYMENT_FAILED - Guest ${body[guestName]} payment was declined")
                    .process(exchange -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> body = exchange.getIn().getBody(Map.class);
                        
                        String subject = "Payment Failed - Holmes Hotel Reservation";
                        String bodyText = "Dear " + body.getOrDefault("guestName", "Guest") + ",\n\n" +
                            "Your payment method was declined or the transaction could not be processed.\n" +
                            "Your booking has not been finalized.\n\n" +
                            "Please update your payment information and try again:\n" +
                            "https://holmeshotel.com/manage-booking\n\n" +
                            "If you continue to experience issues, please contact:\n" +
                            "support@holmeshotel.com\n\n" +
                            "Best regards,\n" +
                            "Holmes Hotel Team";
                        
                        body.put("subject", subject);
                        body.put("body", bodyText);
                        exchange.getIn().setBody(body);
                    })
                    .to("direct:logMockEmail")
                
                // Case 4: Booking Confirmed
                .when().simple("${body[messageName]} == 'BOOKING_CONFIRMED'")
                    
                    .log("✅ [Email Route] BOOKING_CONFIRMED - Guest ${body[guestName]} booking is confirmed")
                    .process(exchange -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> body = exchange.getIn().getBody(Map.class);
                        
                        String subject = "Booking Confirmation - Welcome to Holmes Hotel";
                        String roomType = (String) body.getOrDefault("roomType", "your room");
                        String checkInDate = (String) body.getOrDefault("checkInDate", "N/A");
                        String checkOutDate = (String) body.getOrDefault("checkOutDate", "N/A");
                        
                        String bodyText = "Dear " + body.getOrDefault("guestName", "Guest") + ",\n\n" +
                            "Thank you for booking with Holmes Hotel!\n\n" +
                            "✅ Your reservation has been confirmed.\n\n" +
                            "Booking Details:\n" +
                            "  Room Type: " + roomType + "\n" +
                            "  Check-in: " + checkInDate + "\n" +
                            "  Check-out: " + checkOutDate + "\n\n" +
                            "Your confirmation number and detailed itinerary are attached.\n" +
                            "We look forward to welcoming you!\n\n" +
                            "Best regards,\n" +
                            "Holmes Hotel Team";
                        
                        body.put("subject", subject);
                        body.put("body", bodyText);
                        exchange.getIn().setBody(body);
                    })
                    .to("direct:logMockEmail")


            // Case 5: Service Questionnaire - Request feedback from guest
                .when().simple("${body[messageName]} == 'SERVICE_QUESTIONNAIRE_REQUEST'")
                    .log("📝 [Email Route] SERVICE_QUESTIONNAIRE_REQUEST - Sending feedback form to ${body[guestName]}")
                    .process(exchange -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> body = exchange.getIn().getBody(Map.class);
                        
                        String subject = "We value your feedback - Holmes Hotel";
                        String randomRefCode = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                        
                        String bodyText = "Dear " + body.getOrDefault("guestName", "Guest") + ",\n\n" +
                            "We hope you enjoyed your recent experience at Holmes Hotel.\n\n" +
                            "We are constantly striving to improve our services and would be incredibly grateful if you could take a few minutes to share your thoughts with us.\n\n" +
                            "Please fill out our short service questionnaire by clicking the secure link below:\n" +
                            "https://holmeshotel.com/feedback?bookingRef=" + randomRefCode + "\n\n" +
                            "Thank you for your time and for choosing Holmes Hotel. We hope to see you again soon!\n\n" +
                            "Best regards,\n" +
                            "Holmes Hotel Team";
                        
                        body.put("subject", subject);
                        body.put("body", bodyText);
                        exchange.getIn().setBody(body);
                    })
                    .to("direct:logMockEmail")

             // Case 6: Post-Recovery Questionnaire - Follow-up after complaint resolution
                .when().simple("${body[messageName]} == 'POST_RECOVERY_QUESTIONNAIRE'")
                    .log("📝 [Email Route] POST_RECOVERY_QUESTIONNAIRE - Sending recovery feedback to ${body[guestName]}")
                    .process(exchange -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> body = exchange.getIn().getBody(Map.class);
                        
                        String subject = "How did we do? Your feedback matters - Holmes Hotel";
                        String refCode = (String) body.getOrDefault("reservationCode", "REC-REF-001");
                        
                        String bodyText = "Dear " + body.getOrDefault("guestName", "Guest") + ",\n\n" +
                            "We are happy to hear that our team was able to address your recent concern.\n" +
                            "Could you please spare a moment to let us know if you were satisfied with how we handled your request?\n\n" +
                            "Please fill out the short recovery follow-up here:\n" +
                            "https://holmeshotel.com/recovery-feedback?bookingRef=" + refCode + "\n\n" +
                            "Thank you for giving us a second chance.\n\n" +
                            "Warm regards,\n" +
                            "Holmes Hotel Management";
                        
                        body.put("subject", subject);
                        body.put("body", bodyText);
                        exchange.getIn().setBody(body);
                    })
                    .to("direct:logMockEmail")

            // Case 7: Apology for Delay (Triggered by SLA timer)
                .when().simple("${body[messageName]} == 'SERVICE_DELAYED_APOLOGY'")
                    .log("⏳ [Email Route] SERVICE_DELAYED_APOLOGY - Sending apology to ${body[guestName]}")
                    .process(exchange -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> body = exchange.getIn().getBody(Map.class);
                        
                        String subject = "Update regarding your service request - Holmes Hotel";
                        String bodyText = "Dear " + body.getOrDefault("guestName", "Guest") + ",\n\n" +
                            "We sincerely apologize for the delay in fulfilling your recent service request.\n" +
                            "We are experiencing higher demand than usual, and your request is taking longer than our standard 20-minute SLA.\n\n" +
                            "Our team is prioritizing your order and will have it delivered to your room as soon as possible.\n" +
                            "We appreciate your patience and understanding.\n\n" +
                            "Best regards,\n" +
                            "Holmes Hotel Management";
                        
                        body.put("subject", subject);
                        body.put("body", bodyText);
                        exchange.getIn().setBody(body);
                    })
                    .to("direct:logMockEmail")

            
            // Default case: Unknown message type
            .otherwise()
                .log("⚠️ [Email Route] Unknown messageName: ${body[messageName]}")
            .end();

           
        // Route: Log Mock Email
        from("direct:logMockEmail")
            .routeId("logMockEmailRoute") 
            .process(exchange -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = exchange.getIn().getBody(Map.class);
                
                String to = (String) body.getOrDefault("email", "guest@example.com");
                String subject = (String) body.getOrDefault("subject", "Holmes Hotel Notification");
                String emailBody = (String) body.getOrDefault("body", "");
                
                // Create beautifully formatted ASCII email box
                String mockEmail = buildAsciiEmailBox(to, subject, emailBody);
                
                // Print to console
                System.out.println(mockEmail);
                
                exchange.getIn().setHeader("emailSent", true);
            })
            
            .end();
    }

    /**
     * Build a beautifully formatted ASCII email box for console output
     * 
     * @param to recipient email address
     * @param subject email subject
     * @param body email body content
     * @return formatted ASCII email box
     */
    private String buildAsciiEmailBox(String to, String subject, String body) {
        String separator = "═══════════════════════════════════════════════════════════════════════════════";
        String from = "noreply@holmeshotel.com";
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        StringBuilder emailBox = new StringBuilder();
        emailBox.append("\n");
        emailBox.append("╔").append(separator).append("╗\n");
        emailBox.append("║                           📧 MOCK EMAIL NOTIFICATION 📧                         ║\n");
        emailBox.append("╚").append(separator).append("╝\n");
        emailBox.append("\n");
        emailBox.append("┌─ Email Header ─────────────────────────────────────────────────────────────────┐\n");
        emailBox.append("│                                                                                 │\n");
        emailBox.append("│  FROM:      ").append(padRight(from, 63)).append("  │\n");
        emailBox.append("│  TO:        ").append(padRight(to, 63)).append("  │\n");
        emailBox.append("│  SENT:      ").append(padRight(timestamp, 63)).append("  │\n");
        emailBox.append("│  SUBJECT:   ").append(padRight(subject, 63)).append("  │\n");
        emailBox.append("│                                                                                 │\n");
        emailBox.append("├─ Email Body ──────────────────────────────────────────────────────────────────┤\n");
        emailBox.append("│                                                                                 │\n");
        
        // Wrap body text to fit in 83 character box
        String[] lines = body.split("\n");
        for (String line : lines) {
            if (line.length() <= 79) {
                emailBox.append("│  ").append(padRight(line, 77)).append("  │\n");
            } else {
                // Handle long lines by wrapping them
                int index = 0;
                while (index < line.length()) {
                    int endIndex = Math.min(index + 77, line.length());
                    String segment = line.substring(index, endIndex);
                    emailBox.append("│  ").append(padRight(segment, 77)).append("  │\n");
                    index = endIndex;
                }
            }
        }
        
        emailBox.append("│                                                                                 │\n");
        emailBox.append("└─────────────────────────────────────────────────────────────────────────────────┘\n");
        emailBox.append("\n");
        
        return emailBox.toString();
    }

    /**
     * Pad a string to the right with spaces
     * 
     * @param str the string to pad
     * @param length target length
     * @return padded string
     */
    private String padRight(String str, int length) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= length) {
            return str.substring(0, length);
        }
        return String.format("%-" + length + "s", str);
    }
}
