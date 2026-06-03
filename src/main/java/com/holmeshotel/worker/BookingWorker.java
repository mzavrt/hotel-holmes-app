package com.holmeshotel.worker;

import com.holmeshotel.entity.Reservation;
import com.holmeshotel.repository.ReservationRepository;
import com.holmeshotel.repository.RoomRepository;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Component
public class BookingWorker {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

   @JobWorker(type = "attempt-reservation-hold")
public Map<String, Object> handleAttemptHold(final ActivatedJob job) {
    String requestedType = (String) job.getVariable("requestedRoomType");
    LocalDate checkInDate = LocalDate.parse((String) job.getVariable("checkInDate"));
    LocalDate checkOutDate = LocalDate.parse((String) job.getVariable("checkOutDate"));
    
    // 1. Check Availability
    long totalRooms = roomRepository.countByType(requestedType);
    long overlapping = reservationRepository.countOverlappingReservations(requestedType, checkInDate, checkOutDate);
    boolean holdSuccessful = (totalRooms - overlapping) > 0;

    if (holdSuccessful) {
        // 2. Determine Season (Option B logic)
        // Let's define High Season as June, July, August, and December
        java.time.Month month = checkInDate.getMonth();
        boolean isHighSeason = (month == java.time.Month.JUNE || 
                                month == java.time.Month.JULY || 
                                month == java.time.Month.AUGUST || 
                                month == java.time.Month.DECEMBER);
        
        String season = isHighSeason ? "High" : "Low";  
        
        String newReference = "RES-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Reservation hold = new Reservation();
        hold.setBookingReference(newReference);
        hold.setRoomType(requestedType);
        // ... set other variables ...
        hold.setStatus("PENDING_PAYMENT"); 
        reservationRepository.save(hold);
        
        // Return everything to Camunda!
        return Map.of(
            "holdSuccessful", true, 
            "bookingReference", newReference,
            "season", season
       
        );
    } else {
        return Map.of("holdSuccessful", false);
    }
}
}
