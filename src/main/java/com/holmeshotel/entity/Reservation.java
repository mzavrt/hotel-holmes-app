package com.holmeshotel.entity;

import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Readable ID (Business Key)
    @Column(name = "reservation_code", unique = true, nullable = false, updatable = false)
    private String reservationCode; 

    // Guest Details
    private String guestName;
    private String email;
    private String phoneNumber; 

    // Stay Details
    private String roomType;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String status; // "HELD", "CONFIRMED", "CHECKED_IN", "CANCELLED"

    // Business Data 
    @Column(length = 500)
    private String guestPreferences;
    
    private String preOrderedAmenities;
    private Double calculatedPrice;

    // The specific physical room given at check-in
    private String assignedRoomNumber;

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getReservationCode() { return reservationCode; }
    public void setReservationCode(String reservationCode) { this.reservationCode = reservationCode; }
    
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }  
    
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    
    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
    
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGuestPreferences() { return guestPreferences; }
    public void setGuestPreferences(String guestPreferences) { this.guestPreferences = guestPreferences; }

    public String getPreOrderedAmenities() { return preOrderedAmenities; }
    public void setPreOrderedAmenities(String preOrderedAmenities) { this.preOrderedAmenities = preOrderedAmenities; }

    public Double getCalculatedPrice() { return calculatedPrice; }
    public void setCalculatedPrice(Double calculatedPrice) { this.calculatedPrice = calculatedPrice; }

    public String getAssignedRoomNumber() { return assignedRoomNumber; }
    public void setAssignedRoomNumber(String assignedRoomNumber) { this.assignedRoomNumber = assignedRoomNumber; }

}