package com.holmeshotel.worker;

import com.holmeshotel.entity.Complaint;
import com.holmeshotel.repository.ComplaintRepository;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.annotation.JobWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ComplaintWorker {

    @Autowired
    private ComplaintRepository complaintRepository;

    // ====================================================================================
    // WORKER 1: Initial Intake (Linked to "Log Complaint to Database" Service Task)
    // ====================================================================================
    @JobWorker(type = "log-complaint-db")
    public Map<String, Object> handleLogComplaint(final ActivatedJob job) {
        
        String guestName = (String) job.getVariable("guestName");
        String roomNumber = (String) job.getVariable("roomNumber");
        String email = (String) job.getVariable("email");
        String dateOfOccurrenceStr = (String) job.getVariable("dateOfOccurrence");
        String incidentType = (String) job.getVariable("incidentType");
        String complaintDetails = (String) job.getVariable("complaintDetails");

        // 1. Create and Save the Initial Record
        Complaint complaint = new Complaint();
        complaint.setGuestName(guestName);
        complaint.setRoomNumber(roomNumber);
        complaint.setEmail(email);
        
        // Parse date safely (Camunda usually sends dates as ISO strings like "2026-10-15T00:00:00.000Z")
        if (dateOfOccurrenceStr != null && !dateOfOccurrenceStr.isEmpty()) {
            complaint.setDateOfOccurrence(LocalDate.parse(dateOfOccurrenceStr.substring(0, 10)));
        }
        
        complaint.setIncidentType(incidentType);
        complaint.setComplaintDetails(complaintDetails);
        complaint.setStatus("OPEN");
        complaint.setLoggedAt(LocalDateTime.now());
        
        Complaint savedComplaint = complaintRepository.save(complaint);

        System.out.println("🚨 [Intake] Complaint logged for Room " + roomNumber + ". DB ID: " + savedComplaint.getId());

        // 2. Return the DB ID to Camunda! 
        Map<String, Object> variables = new HashMap<>();
        variables.put("complaintDbId", savedComplaint.getId());
        return variables;
    }

    // ====================================================================================
    // WORKER 2: The Final Update (Linked to "Log Complaint and Resolution" Service Task)
    // ====================================================================================
    @JobWorker(type = "log-resolution-db")
    public void handleLogResolution(final ActivatedJob job) {
        
        // Grab the Database ID we saved in Worker 1
        Long complaintDbId = ((Number) job.getVariable("complaintDbId")).longValue();
        
        // Variables set by the manager during their User Tasks
        String isRepetitive = (String) job.getVariable("isRepetitive");
        String resolutionAction = (String) job.getVariable("resolutionAction"); 

        System.out.println("💾 [Resolution] Attempting to update Complaint ID: " + complaintDbId);

        // Fetch the EXISTING record from the database
        Optional<Complaint> complaintOpt = complaintRepository.findById(complaintDbId);

        if (complaintOpt.isPresent()) {
            Complaint complaint = complaintOpt.get();
            
            // Update the record to reflect the manager's check and the final fix
            complaint.setIsRepetitive(isRepetitive);
            complaint.setStatus("RESOLVED");
            complaint.setResolutionAction(resolutionAction);
            complaint.setResolvedAt(LocalDateTime.now());
            
            complaintRepository.save(complaint);
            System.out.println("✅ [Resolution] Successfully closed complaint for " + complaint.getGuestName() + ". Action taken: " + resolutionAction);
        } else {
            System.err.println("❌ [Resolution] ERROR: Could not find Complaint ID " + complaintDbId + " in the database.");
        }
    }
}