package com.holmeshotel.controller;

import io.camunda.client.CamundaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class BookingWebhookController {

    @Autowired
    private CamundaClient camundaClient;

    @PostMapping("/booking")
    public Map<String, Object> receiveExternalBooking(@RequestBody Map<String, Object> incomingPayload) {
        
        // 1. Prepare the payload (NO bookingReference is generated here anymore!)
        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("guestName", incomingPayload.get("guestName"));
        processVariables.put("requestedRoomType", incomingPayload.get("requestedRoomType"));
        processVariables.put("checkInDate", incomingPayload.get("checkInDate"));
        processVariables.put("checkOutDate", incomingPayload.get("checkOutDate"));
        processVariables.put("source", "External Web Platform");

        try {
            // 2. Trigger the Message Start Event in Camunda
            camundaClient.newPublishMessageCommand()
                    .messageName("Msg_NewBooking")
                    .correlationKey("") // Empty because this is a Start Event
                    .variables(processVariables)
                    .send()
                    .join();

            System.out.println("✅ [Webhook] Booking Request successfully injected into Camunda.");

            // 3. Return an acknowledgment to the website
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
}