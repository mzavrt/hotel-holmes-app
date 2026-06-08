package com.holmeshotel.worker;

import com.holmeshotel.entity.Incident;
import com.holmeshotel.repository.IncidentRepository;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.annotation.JobWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ComplaintWorker {

    @Autowired
    private IncidentRepository incidentRepository;

    // ====================================================================================
    // WORKER 1: Initial Intake (Linked to "Log Complaint to Database" Service Task)
    // ====================================================================================
    @JobWorker(type = "log-complaint-db")
    public Map<String, Object> handleLogComplaint(final ActivatedJob job) {
        
        String guestName = (String) job.getVariable("guestName");
        String roomNumber = (String) job.getVariable("roomNumber");
        String complaintText = (String) job.getVariable("complaintText");

        // 1. Create and Save the Initial Record
        Incident incident = new Incident();
        incident.setGuestName(guestName);
        incident.setRoomNumber(roomNumber);
        incident.setComplaintText(complaintText);
        incident.setStatus("OPEN");
        incident.setLoggedAt(LocalDateTime.now());
        
        Incident savedIncident = incidentRepository.save(incident);

        System.out.println("🚨 [Intake] Complaint logged for Room " + roomNumber + ". DB ID: " + savedIncident.getId());

        // 2. Return the DB ID to Camunda! 
        // We need this so the final resolution worker knows which row to update.
        Map<String, Object> variables = new HashMap<>();
        variables.put("incidentDbId", savedIncident.getId());
        return variables;
    }

    // ====================================================================================
    // WORKER 2: The Final Update (Linked to "Log Complaint and Resolution" Service Task)
    // ====================================================================================
    @JobWorker(type = "log-resolution-db")
    public void handleLogResolution(final ActivatedJob job) {
        
        // Grab the Database ID we saved in Worker 1
        Long incidentDbId = ((Number) job.getVariable("incidentDbId")).longValue();
        
        // This variable is set by the manager during their User Task (e.g., "Issued 5% Coupon")
        String resolutionAction = (String) job.getVariable("resolutionAction"); 

        System.out.println("💾 [Resolution] Attempting to update Incident ID: " + incidentDbId);

        // Fetch the EXISTING record from the database
        Optional<Incident> incidentOpt = incidentRepository.findById(incidentDbId);

        if (incidentOpt.isPresent()) {
            Incident incident = incidentOpt.get();
            
            // Update the record to reflect that the staff fixed it
            incident.setStatus("RESOLVED");
            incident.setResolutionAction(resolutionAction);
            incident.setResolvedAt(LocalDateTime.now());
            
            incidentRepository.save(incident);
            System.out.println("✅ [Resolution] Successfully closed incident for " + incident.getGuestName() + ". Action taken: " + resolutionAction);
        } else {
            System.err.println("❌ [Resolution] ERROR: Could not find Incident ID " + incidentDbId + " in the database.");
        }
    }

    // ====================================================================================
    // WORKER 3: SendGrid Dispatch (Linked to "Request to fill-in Post-Recovery Questionnaire")
    // ====================================================================================
    @JobWorker(type = "send-survey-email")
    public void handleSendSurvey(final ActivatedJob job) {
        
        String guestEmail = (String) job.getVariable("guestEmail");
        String guestName = (String) job.getVariable("guestName");

        System.out.println("📧 [SendGrid] Initiating API call to send Post-Recovery Survey to: " + guestEmail);
        
        // SendGrid API logic will go here
    }
}