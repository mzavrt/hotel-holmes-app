package com.holmeshotel.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.Random;

@RestController
@RequestMapping("/api/external/mock-kds")
public class MockKDSController {

    /**
     * Endpoint: POST /api/external/mock-kds/send-order
     * Simulates pushing a ticket to the physical screens in the hotel kitchen.
     */
    @PostMapping("/send-order")
    public ResponseEntity<KdsResponse> processKitchenOrder(@RequestBody KdsRequest request) {
        
        KdsResponse response = new KdsResponse();
        // Generate a realistic-looking kitchen ticket ID (e.g., "KDS-A8F93B")
        response.setKitchenTicketId("KDS-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        // MOCK LOGIC: We want to be able to test the "Rejected" path in Camunda.
        // If the special instructions contain the word "reject" or the room is 999 (a test room), decline it.
        if ((request.getRoomNumber() != null && request.getRoomNumber() == 999) || 
            (request.getSpecialInstructions() != null && request.getSpecialInstructions().toLowerCase().contains("reject"))) {
            
            response.setStatus("REJECTED");
            response.setEstimatedPrepTimeMinutes(0);
            response.setMessage("Order rejected: Kitchen is out of stock for requested items.");
            
            return ResponseEntity.ok(response); 
            // Note: Returning 200 OK so Camunda's REST connector maps the response cleanly, 
            // allowing your BPMN gateways to evaluate the 'status' variable.
        }

        // Default happy path
        response.setStatus("ACCEPTED");
        // Simulate a realistic prep time between 10 and 25 minutes
        response.setEstimatedPrepTimeMinutes(new Random().nextInt(16) + 10);
        response.setMessage("Order successfully pushed to kitchen screens.");
        
        return ResponseEntity.ok(response);
    }

    // --- Data Transfer Objects (DTOs) ---

    public static class KdsRequest {
        private String orderId;
        private Integer roomNumber;
        private String orderCategory;
        private String orderItems;
        private String specialInstructions;

        // Getters and Setters
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public Integer getRoomNumber() { return roomNumber; }
        public void setRoomNumber(Integer roomNumber) { this.roomNumber = roomNumber; }
        public String getOrderCategory() { return orderCategory; }
        public void setOrderCategory(String orderCategory) { this.orderCategory = orderCategory; }
        public String getOrderItems() { return orderItems; }
        public void setOrderItems(String orderItems) { this.orderItems = orderItems; }
        public String getSpecialInstructions() { return specialInstructions; }
        public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }
    }

    public static class KdsResponse {
        private String kitchenTicketId;
        private String status;
        private Integer estimatedPrepTimeMinutes;
        private String message;

        // Getters and Setters
        public String getKitchenTicketId() { return kitchenTicketId; }
        public void setKitchenTicketId(String kitchenTicketId) { this.kitchenTicketId = kitchenTicketId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getEstimatedPrepTimeMinutes() { return estimatedPrepTimeMinutes; }
        public void setEstimatedPrepTimeMinutes(Integer estimatedPrepTimeMinutes) { this.estimatedPrepTimeMinutes = estimatedPrepTimeMinutes; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}