package com.holmeshotel.worker;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.worker.JobClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

@Component
public class PaymentWorker {

    private static final String PAYMENT_URL = "https://unissued-bubbly-slick.ngrok-free.dev/api/external/mock-payment/charge";

    @Autowired
    private RestTemplate restTemplate;

    @JobWorker(type = "process-payment", autoComplete = false)
    public void handleProcessPayment(final JobClient client, final ActivatedJob job) {

        // 1. Extract payment variables from the Camunda process
        Object priceObj = job.getVariable("calculatedPrice");
        Double amount = (priceObj != null) ? ((Number) priceObj).doubleValue() : 0.0;

        String cardNumber = (String) job.getVariable("cardNumber");
        String reservationCode = (String) job.getVariable("reservationCode");

        // 2. Build the request payload for the Mock Payment Gateway
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("reservationCode", reservationCode);
        paymentRequest.put("amount", amount);
        paymentRequest.put("cardNumber", cardNumber);

        try {
            // 3. Call the Mock Payment Gateway
            System.out.println("💳 [PaymentWorker] Calling Mock Payment Gateway for reservation: "
                + reservationCode + ", amount: €" + amount);

            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(
                PAYMENT_URL, paymentRequest, Map.class
            );

            Map<String, Object> response = responseEntity.getBody();
            String status = (String) response.get("status");
            String transactionId = (String) response.get("transactionId");

            System.out.println("🔍 [PaymentWorker] Full response: " + response);
            System.out.println("🔍 [PaymentWorker] Status: " + status);
            System.out.println("🔍 [PaymentWorker] TransactionId: " + transactionId);

            if ("APPROVED".equals(status)) {
                // 4a. Happy path — complete the job and pass variables back to Camunda
                Map<String, Object> outputVariables = new HashMap<>();
                outputVariables.put("paymentStatus", "APPROVED");
                outputVariables.put("transactionId", transactionId);

                client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

                System.out.println("✅ [PaymentWorker] Payment approved. Transaction ID: " + transactionId);

            } else {
                // 4b. Business decline — throw BPMN error for error boundary event
                System.out.println("❌ [PaymentWorker] Payment declined by gateway.");

                Map<String, Object> outputVariables = new HashMap<>();
                outputVariables.put("paymentStatus", "DECLINED");
                outputVariables.put("transactionId", transactionId);

                client.newThrowErrorCommand(job.getKey())
                    .errorCode("PAYMENT_DECLINED")
                    .errorMessage("Payment was declined by the gateway: " + response.get("message"))
                    .send()
                    .join();
            }

         
        } catch (Exception e) {
            // 6. Unexpected error — fail the job so Camunda can retry
            System.err.println("❌ [PaymentWorker] Unexpected error during payment: " + e.getMessage());

           client.newThrowErrorCommand(job.getKey())
                .errorCode("PAYMENT_SERVICE_UNAVAILABLE")
                .errorMessage("Mock Payment Gateway could not be reached: " + e.getMessage())
                .send()
                .join();
        }
    }
}