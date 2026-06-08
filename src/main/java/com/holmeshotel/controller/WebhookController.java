package com.holmeshotel.controller;

import io.camunda.client.CamundaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    @Autowired
    private CamundaClient camundaClient;

    @PostMapping("/service-order")
    public Map<String, Object> receiveInRoomServiceOrder(@RequestBody Map<String, Object> incomingPayload) {
        
        // 1. Prepare the payload
        Map<String, Object> processVariables = new HashMap<>();
        
        // Extract standard guest and room identifiers
        processVariables.put("roomNumber", incomingPayload.get("roomNumber"));
        processVariables.put("guestName", incomingPayload.get("guestName"));
        
        // The critical variable that controls the XOR Gateway ("food" vs "recreation")
        processVariables.put("orderCategory", incomingPayload.get("orderCategory"));
        
        // The specifics of what the guest actually wants
        processVariables.put("orderItems", incomingPayload.get("orderItems"));
        
        // Handle optional text areas and numeric fields safely
        if (incomingPayload.containsKey("specialInstructions")) {
            processVariables.put("specialInstructions", incomingPayload.get("specialInstructions"));
        }
        
        if (incomingPayload.containsKey("estimatedTotal")) {
            processVariables.put("estimatedTotal", incomingPayload.get("estimatedTotal"));
        }

        try {
            // 2. Trigger the Message Start Event in Camunda
            camundaClient.newPublishMessageCommand()
                    .messageName("ServiceOrderReceived")
                    .correlationKey("") // Empty string matches the start event pattern
                    .variables(processVariables)
                    .send()
                    .join();

            System.out.println("✅ [Webhook] In-Room Service Order successfully injected into Camunda.");

            // 3. Return an acknowledgment
            Map<String, Object> response = new HashMap<>();
            response.put("status", "Accepted");
            response.put("message", "Service order received. The Guest Relations Officer is verifying your request.");
            return response;

        } catch (Exception e) {
            System.err.println("❌ [Webhook] Failed to start service process: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "Error");
            errorResponse.put("message", e.getMessage());
            return errorResponse;
        }
    }

    @PostMapping("/complaint")
    public String receiveComplaint(@RequestBody Map<String, Object> complaintData) {
        
        // Generate a unique correlation key for the message
        String correlationKey = "INC-" + UUID.randomUUID().toString();

        // Publish the message to Camunda to start Process 7
        camundaClient.newPublishMessageCommand()
            .messageName("Msg_NewComplaint")
            .correlationKey(correlationKey)
            .variables(complaintData)
            .send()
            .join();

        return "Complaint received and process started with Key: " + correlationKey;
    }

}