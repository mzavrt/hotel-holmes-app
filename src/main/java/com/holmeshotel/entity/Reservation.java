package com.holmeshotel.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Reservation {
    @Id
    private String bookingReference; // e.g., "RES-8F7A92"
    private String guestName;
    private String email;
    private String phoneNumber; // Added for the Twilio/Assa Abloy API
    private String roomType;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String status; // "CONFIRMED", "CHECKED_IN", "CANCELLED"

    // Getters and Setters
    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }  
    
}