package com.holmeshotel.worker;

import com.holmeshotel.entity.Reservation;
import com.holmeshotel.repository.ReservationRepository;
import com.holmeshotel.repository.RoomRepository;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    String requestedType = (String) job.getVariable("roomType");
    LocalDate checkInDate = LocalDate.parse((String) job.getVariable("checkInDate"));
    LocalDate checkOutDate = LocalDate.parse((String) job.getVariable("checkOutDate"));

    // Calculate the number of nights
    long numberOfNights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    
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
        
       String code = "RES-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
Reservation hold = new Reservation();

// Update this method call (assuming you updated your Entity class too)
hold.setReservationCode(code); 
hold.setRoomType(requestedType);
hold.setCheckInDate(checkInDate);
hold.setCheckOutDate(checkOutDate);
hold.setStatus("HELD");
reservationRepository.save(hold);

// Return 'reservationCode' to Camunda
return Map.of(
    "holdSuccessful", true, 
    "reservationCode", code, // <-- Camunda will now hold this variable
    "season", season,
    "numberOfNights", numberOfNights
);
    } else {
        return Map.of("holdSuccessful", false);
    }
}

@JobWorker(type = "update-reservation-paid")
    public void handleUpdateReservationPaid(final ActivatedJob job) {
        // 1. Get the reservation code to find the correct database row
        String reservationCode = (String) job.getVariable("reservationCode");

        // 2. Fetch the existing "HELD" reservation from the database
        Reservation reservation = reservationRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new RuntimeException("Reservation " + reservationCode + " not found in database!"));

        // 3. Extract the business data gathered during the Camunda process
        String guestName = (String) job.getVariable("guestName");
        String email = (String) job.getVariable("email");
        String phoneNumber = (String) job.getVariable("phoneNumber");
        
        Object priceObj = job.getVariable("calculatedPrice");
        Double calculatedPrice = (priceObj != null) ? ((Number) priceObj).doubleValue() : 0.0;

        String amenitiesDetails = (String) job.getVariable("amenitiesDetails");

        // --- EXPERT TRICK: Handle the Checkbox List ---
        // Camunda stores the checkboxes as a List<String>, but our DB expects a single String.
        // We use String.join() to convert ["Massage", "Transport"] into "Massage, Transport"
        Object amenitiesObj = job.getVariable("selectedAmenities");
        if (amenitiesObj instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<String> amenitiesList = (java.util.List<String>) amenitiesObj;
            String joinedAmenities = String.join(", ", amenitiesList);
            reservation.setSelectedAmenities(joinedAmenities);
        }

        // 4. Update the entity with the permanent data
        reservation.setStatus("CONFIRMED");
        reservation.setGuestName(guestName);
        reservation.setEmail(email);
        reservation.setPhoneNumber(phoneNumber);
        reservation.setCalculatedPrice(calculatedPrice);

        reservation.setAmenitiesDetails(amenitiesDetails);

        // 5. Save back to the database!
        reservationRepository.save(reservation);
        
        System.out.println("✅ Reservation " + reservationCode + " successfully updated to CONFIRMED.");
        
        // Note: The method returns void because we don't need to pass any new variables back to Camunda.
        // Camunda will automatically complete the task and move to the next step.
    }
}
