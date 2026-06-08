package com.holmeshotel.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guest & Incident Details (From the intake form)
    private String guestName;
    private String roomNumber;
    private String email;
    private LocalDate dateOfOccurrence;
    private String incidentType;
    private String complaintDetails;

    // Manager Verification
    private String isRepetitive; // "Yes" or "No"

    // Status tracking (e.g., "OPEN", "RESOLVED")
    private String status; 
    private LocalDateTime loggedAt;
    
    // Resolution tracking (updated at the end of the process)
    private String resolutionAction;
    private LocalDateTime resolvedAt;

    // ==========================================
    // Empty Constructor required by JPA/Hibernate
    // ==========================================
    public Complaint() {
    }

    // ==========================================
    // Getters and Setters
    // ==========================================
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getDateOfOccurrence() {
        return dateOfOccurrence;
    }

    public void setDateOfOccurrence(LocalDate dateOfOccurrence) {
        this.dateOfOccurrence = dateOfOccurrence;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public String getComplaintDetails() {
        return complaintDetails;
    }

    public void setComplaintDetails(String complaintDetails) {
        this.complaintDetails = complaintDetails;
    }

    public String getIsRepetitive() {
        return isRepetitive;
    }

    public void setIsRepetitive(String isRepetitive) {
        this.isRepetitive = isRepetitive;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }

    public String getResolutionAction() {
        return resolutionAction;
    }

    public void setResolutionAction(String resolutionAction) {
        this.resolutionAction = resolutionAction;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}