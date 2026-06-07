package com.holmeshotel.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_order")
public class ServiceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Readable ID / Business Key (e.g., "SRV-A8F93B")
    @Column(name = "transaction_id", unique = true, nullable = false, updatable = false)
    private String transactionId; 

    // Guest & Location
    private Integer roomNumber;
    private String guestName;

    // Order Details
    private String orderCategory; // "Food" or "Recreation"
    
    @Column(length = 1000)
    private String orderItems;
    
    @Column(length = 500)
    private String specialInstructions;

    // Billing
    private Double amountBilled;
    private LocalDateTime fulfillmentTime;
    private String status; // "BILLED_TO_ROOM"

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public Integer getRoomNumber() { return roomNumber; }
    public void setRoomNumber(Integer roomNumber) { this.roomNumber = roomNumber; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getOrderCategory() { return orderCategory; }
    public void setOrderCategory(String orderCategory) { this.orderCategory = orderCategory; }

    public String getOrderItems() { return orderItems; }
    public void setOrderItems(String orderItems) { this.orderItems = orderItems; }

    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }

    public Double getAmountBilled() { return amountBilled; }
    public void setAmountBilled(Double amountBilled) { this.amountBilled = amountBilled; }

    public LocalDateTime getFulfillmentTime() { return fulfillmentTime; }
    public void setFulfillmentTime(LocalDateTime fulfillmentTime) { this.fulfillmentTime = fulfillmentTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}