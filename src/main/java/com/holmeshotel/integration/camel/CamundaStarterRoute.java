package com.holmeshotel.integration.camel;

import io.camunda.client.CamundaClient;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Camunda Starter Route
 * 
 * This is the central hub route that receives normalized booking data from any of the three
 * channels (JSON REST, XML REST, or CSV File) and triggers a Camunda process message.
 * 
 * This route implements the omnichannel integration pattern where all message sources
 * converge into a single normalized format before interacting with the BPM engine.
 * 
 * EIP Patterns Used:
 * - Aggregator: Convergence point for all three channels
 * - Service Activator: Calls the Camunda client to trigger a business process
 * - Request-Reply: Sends request to Camunda and receives acknowledgment
 * - Error Handler: Captures failures and returns error responses
 */
@Component
public class CamundaStarterRoute extends RouteBuilder {

    @Autowired
    private CamundaClient camundaClient;

    @Override
    public void configure() throws Exception {

        // Configure error handling for Camunda integration failures
        onException(Exception.class)
            .routeId("camundaErrorHandler")
            .handled(true)
            .log("❌ [Camunda] Error triggering Camunda process: ${exception.message}")
            .process(exchange -> {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "Error");
                errorResponse.put("message", exchange.getProperty(Exception.class.getName(), Exception.class).getMessage());
                exchange.getIn().setBody(errorResponse);
            });

        // Route: Central Camunda message trigger
        // This route is the convergence point for all three channels
        from("direct:startCamunda")
            .routeId("camundaStarterRoute")
            .log("🔄 [Camunda Gateway] Normalizing booking data from source: ${body[source]}")
            
            // EIP: Service Activator - call Camunda to trigger process
            .process(exchange -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> bookingData = exchange.getIn().getBody(Map.class);
                
                if (bookingData == null) {
                    throw new RuntimeException("No booking data found in exchange body");
                }
                
                // Extract source information for logging/tracking
                String source = (String) bookingData.getOrDefault("source", "Unknown");
                String sourceFile = (String) bookingData.get("sourceFile");
                
                // Generate unique correlation key for message correlation (optional but recommended)
                String correlationKey = "BOOKING-" + UUID.randomUUID().toString();
                
                try {
                    // Log the booking attempt
                    String guestName = (String) bookingData.get("guestName");
                    System.out.println("📤 [Camunda] Triggering Camunda process for guest: " + guestName);
                    System.out.println("   Source: " + source + (sourceFile != null ? " (File: " + sourceFile + ")" : ""));
                    
                    // Send message to Camunda to start the booking process
                    // The message name "Msg_NewBooking" must match a Message Start Event in the BPMN diagram
                    camundaClient.newPublishMessageCommand()
                        .messageName("Msg_NewBooking")
                        .correlationKey(correlationKey)  // Used for message correlation in Camunda
                        .variables(bookingData)
                        .send()
                        .join();
                    
                    // Create success response
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "Accepted");
                    response.put("message", "Booking request received and injected into process engine");
                    response.put("correlationKey", correlationKey);
                    response.put("processMessage", "Msg_NewBooking");
                    response.put("guestName", bookingData.get("guestName"));
                    response.put("source", source);
                    
                    exchange.getIn().setBody(response);
                    
                    System.out.println("✅ [Camunda] Successfully published message to Camunda");
                    
                } catch (Exception e) {
                    System.err.println("❌ [Camunda] Failed to publish message: " + e.getMessage());
                    throw new RuntimeException("Camunda integration failed: " + e.getMessage(), e);
                }
            })
            
            .log("✅ [Camunda Gateway] Response prepared: ${body}")
            
            .end();
    }
}
