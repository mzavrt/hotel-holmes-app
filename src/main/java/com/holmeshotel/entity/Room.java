package com.holmeshotel.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Room {
    @Id
    private String roomNumber;
    private String type;
    private String status;

    // Getters and Setters
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}