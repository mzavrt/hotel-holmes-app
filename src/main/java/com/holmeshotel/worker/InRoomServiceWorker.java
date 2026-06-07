package com.holmeshotel.worker;

import com.holmeshotel.entity.ServiceOrder;
import com.holmeshotel.repository.ServiceOrderRepository;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.annotation.JobWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class InRoomServiceWorker {

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @JobWorker(type = "insert-billing-database")
    public void handleInsertBillingDatabase(final ActivatedJob job) {
        
        // 1. Extract variables from the Camunda process
        Integer roomNumber = (Integer) job.getVariable("roomNumber");
        String guestName = (String) job.getVariable("guestName");
        String orderCategory = (String) job.getVariable("orderCategory");
        String orderItems = (String) job.getVariable("orderItems");
        String specialInstructions = (String) job.getVariable("specialInstructions");
        
        // Handle numbers safely in case they come through as Integers instead of Doubles
        Object priceObj = job.getVariable("estimatedTotal");
        Double amountBilled = (priceObj != null) ? ((Number) priceObj).doubleValue() : 0.0;

        // 2. Create the Database Entity
        ServiceOrder order = new ServiceOrder();
        
        // Generate a unique transaction ID for the receipt
        String transactionId = "SRV-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        order.setTransactionId(transactionId);
        
        order.setRoomNumber(roomNumber);
        order.setGuestName(guestName);
        order.setOrderCategory(orderCategory);
        order.setOrderItems(orderItems);
        order.setSpecialInstructions(specialInstructions);
        order.setAmountBilled(amountBilled);
        
        // Record the exact time the delivery runner or therapist clicked "Complete"
        order.setFulfillmentTime(LocalDateTime.now());
        order.setStatus("BILLED_TO_ROOM");

        // 3. Save to the H2 Database
        serviceOrderRepository.save(order);
        
        System.out.println("✅ [Database Insert] Service Order " + transactionId + 
                           " successfully billed " + amountBilled + " to Room " + roomNumber);
                           
        // Method returns void. Camunda automatically completes the task and moves to the End Event.
    }
}