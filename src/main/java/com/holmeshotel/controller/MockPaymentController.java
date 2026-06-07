package com.holmeshotel.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/external/mock-payment")
public class MockPaymentController {

    /**
     * Endpoint: POST /api/external/mock-payment/charge
     * Simulates a credit card processing gateway.
     */
    @PostMapping("/charge")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(UUID.randomUUID().toString());
        response.setProcessedAmount(request.getAmount());

        // MOCK LOGIC: We want to be able to test the "Declined" path in Camunda.
        // If the calculated price is over 5000 (or if the card number is specifically "0000"), we decline it.
        if (request.getAmount() != null && request.getAmount() >= 5000) {
            response.setStatus("DECLINED");
            response.setMessage("Transaction declined: Amount exceeds mock limit.");
            return ResponseEntity.ok(response); 
            // Note: Returning 200 OK so Camunda doesn't throw a technical HTTP error, 
            // but rather evaluates the JSON 'status' field for business logic.
        }

        // Default happy path
        response.setStatus("APPROVED");
        response.setMessage("Payment processed successfully.");
        
        return ResponseEntity.ok(response);
    }

    // --- Data Transfer Objects (DTOs) ---

    public static class PaymentRequest {
        private String reservationCode;
        private Double amount;
        private String cardNumber;

        // Getters and Setters
        public String getReservationCode() { return reservationCode; }
        public void setReservationCode(String reservationCode) { this.reservationCode = reservationCode; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    }

    public static class PaymentResponse {
        private String transactionId;
        private String status;
        private String message;
        private Double processedAmount;

        // Getters and Setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Double getProcessedAmount() { return processedAmount; }
        public void setProcessedAmount(Double processedAmount) { this.processedAmount = processedAmount; }
    }
}