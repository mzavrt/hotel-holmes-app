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

    @PostMapping("/booking")
    public Map<String, Object> receiveExternalBooking(@RequestBody Map<String, Object> incomingPayload) {
        
        // 1. Prepare the payload
        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("guestName", incomingPayload.get("guestName"));
        processVariables.put("email", incomingPayload.get("email"));
        processVariables.put("phoneNumber", incomingPayload.get("phoneNumber"));

        processVariables.put("requestedRoomType", incomingPayload.get("requestedRoomType"));
        processVariables.put("checkInDate", incomingPayload.get("checkInDate"));
        processVariables.put("checkOutDate", incomingPayload.get("checkOutDate"));

        processVariables.put("cardNumber", incomingPayload.get("cardNumber"));
        processVariables.put("cardExpiry", incomingPayload.get("cardExpiry"));
        processVariables.put("source", "External Web Platform");

        // --- NEW: Handle Checkboxes and Text Areas ---
        // Extract the array of selected checkboxes (e.g., ["Arrival Transport", "In-Room Massage"])
        // Ensure the variable name exactly matches the "Key" you set in the Camunda Form!
        if (incomingPayload.containsKey("selectedAmenities")) {
            processVariables.put("selectedAmenities", incomingPayload.get("selectedAmenities"));
        }

        // Extract the text area for guest preferences/allergies
        if (incomingPayload.containsKey("amenitiesDetails")) {
            processVariables.put("amenitiesDetails", incomingPayload.get("amenitiesDetails"));
        }

        try {
            // 2. Trigger the Message Start Event in Camunda
            camundaClient.newPublishMessageCommand()
                    .messageName("Msg_NewBooking")
                    .correlationKey("") 
                    .variables(processVariables)
                    .send()
                    .join();

            System.out.println("✅ [Webhook] Booking Request successfully injected into Camunda.");

            // 3. Return an acknowledgment
            Map<String, Object> response = new HashMap<>();
            response.put("status", "Accepted");
            response.put("message", "Request received. The system is generating your booking reference.");
            return response;

        } catch (Exception e) {
            System.err.println("❌ [Webhook] Failed to start process: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "Error");
            errorResponse.put("message", e.getMessage());
            return errorResponse;
        }
    }

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