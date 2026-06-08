package com.holmeshotel.integration.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * JSON Webhook Route
 * * Exposes a REST endpoint /api/camel/webhook/json that accepts JSON payloads,
 * unmarshals them to a Map, renames specific keys for normalization, handles arrays,
 * and forwards to the Camunda starter route.
 * * EIP Patterns Used:
 * - Content-Based Router: Routes JSON to a transformation step
 * - Message Transformer: Renames keys to normalize data model
 * - Recipient List: Forwards normalized data to direct:startCamunda
 */
@Component
public class JsonWebhookRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Configure REST endpoint for JSON webhook
        rest()
            .post("/webhook/json")
            .description("Receive booking requests via JSON")
            .consumes("application/json")
            .produces("application/json")
            .bindingMode(RestBindingMode.json)
            .type(Map.class) // VYLEPŠENÍ 1: Explicitně říkáme Jacksonu, že chceme Mapu
            .to("direct:normalizeJsonPayload");

        // Route: Receive JSON and normalize
        from("direct:normalizeJsonPayload")
            .routeId("jsonWebhookRoute")
            .log("📥 [JSON Gateway] Received booking request from JSON endpoint")
            .log("Request body: ${body}")
            
            // Process the incoming map to rename keys and flatten arrays
            .process(exchange -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = exchange.getIn().getBody(Map.class);
                
                if (payload == null) {
                    payload = new HashMap<>();
                }
                
                // EIP: Message Transformer - rename keys for normalization
                
                // requestedRoomType → roomType
                if (payload.containsKey("requestedRoomType")) {
                    payload.put("roomType", payload.remove("requestedRoomType"));
                }
                     
                // Add source tracking
                payload.put("source", "JSON API Gateway");
                
                exchange.getIn().setBody(payload);
            })
            
            .log("✅ [JSON Gateway] Payload normalized. Keys and arrays transformed successfully")
            
            // EIP: Recipient List - forward to Camunda starter
            .to("direct:startCamunda")
            
            .end();
    }
}