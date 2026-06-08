package com.holmeshotel.worker;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.annotation.JobWorker;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Zeebe Job Worker for Email Message End Events and Tasks
 * 
 * This worker handles all email notifications triggered by the BPMN process:
 * - RESERVATION_DENIED
 * - DETAILS_REJECTED
 * - PAYMENT_FAILED
 * - BOOKING_CONFIRMED
 * - SERVICE_QUESTIONNAIRE_REQUEST
 * - POST_RECOVERY_QUESTIONNAIRE
 * 
 * Each email-sending task/event publishes a job of type "email-sender".
 * The worker extracts the messageName header (set in BPMN task configuration)
 * and uses Camel to route the payload to the appropriate email template handler.
 * 
 * EIP Pattern: Service Activator - Activates external service (Camel email router)
 */
@Component
public class EmailMessageEventWorker {

    @Autowired
    private ProducerTemplate producerTemplate;

    /**
     * Job Worker for handling email sending tasks
     * 
     * Configuration in BPMN:
     * - Job Type: "email-sender"
     * - Custom Headers: messageName = (name of the specific email template)
     * - autoComplete: true (automatically complete job after execution)
     * 
     * @param job the activated Zeebe job containing process context
     */
    @JobWorker(type = "email-sender", autoComplete = true)
    public void sendEmailMessage(ActivatedJob job) {
        
        // Extract the custom header that indicates which email template to use
        String messageName = job.getCustomHeaders().get("messageName");
        
        System.out.println("📧 [Zeebe JobWorker] Processing email job for message: " + messageName);
        
        // Get all process variables from the Zeebe context
        Map<String, Object> processVariables = new HashMap<>(job.getVariablesAsMap());
        
        // EIP: Enrichment - Add the messageName to the payload for routing
        // This allows the Camel route to know which logic to trigger
        processVariables.put("messageName", messageName);
        
        try {
            // EIP: Service Activator - Send to Camel router for email handling
            // The Camel route will use Content-Based Router to select the email template
            producerTemplate.sendBody("direct:mockEmailRouter", processVariables);
            
            System.out.println("✅ [Zeebe JobWorker] Email routing request sent to Camel for: " + messageName);
            
        } catch (Exception e) {
            System.err.println("❌ [Zeebe JobWorker] Failed to route email: " + e.getMessage());
            throw new RuntimeException("Email routing failed: " + e.getMessage(), e);
        }
    }
}