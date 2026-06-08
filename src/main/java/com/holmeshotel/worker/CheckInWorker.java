package com.holmeshotel.worker;

import com.holmeshotel.entity.Reservation;
import com.holmeshotel.entity.Room;
import com.holmeshotel.repository.ReservationRepository;
import com.holmeshotel.repository.RoomRepository;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.annotation.JobWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CheckInWorker {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomRepository roomRepository;

    // ====================================================================================
    // WORKER 1: Search Reservation & Auto-Suggest Rooms (Linked to "Search" Service Task)
    // ====================================================================================
    @JobWorker(type = "search-reservation")
    public Map<String, Object> handleSearchReservation(final ActivatedJob job) {
        
        String searchCode = (String) job.getVariable("searchCode");
        String searchName = (String) job.getVariable("searchName");
        LocalDate today = LocalDate.now();

        Optional<Reservation> foundReservation = Optional.empty();

        // 1. Search Logic
        if (searchCode != null && !searchCode.trim().isEmpty()) {
            Optional<Reservation> res = reservationRepository.findByReservationCode(searchCode);
            if (res.isPresent() && res.get().getCheckInDate().equals(today)) {
                foundReservation = res;
            }
        } else if (searchName != null && !searchName.trim().isEmpty()) {
            List<Reservation> results = reservationRepository.findByGuestNameContainingIgnoreCase(searchName);
            foundReservation = results.stream()
                    .filter(r -> r.getCheckInDate().equals(today))
                    .findFirst();
        }

        Map<String, Object> variables = new HashMap<>();

        // 2. If Found: Fetch Available Rooms for the Dropdown Form
        if (foundReservation.isPresent()) {
            Reservation res = foundReservation.get();
            variables.put("reservationFound", true);
            
            // Basic Guest Details
            variables.put("reservationCode", res.getReservationCode());
            variables.put("guestName", res.getGuestName());
            variables.put("roomType", res.getRoomType());
            variables.put("selectedAmenities", res.getSelectedAmenities());
            variables.put("checkOutDate", res.getCheckOutDate().toString());

            // NEW SCHEMA: Ask the DB for VACANT and CLEAN rooms of this type
            List<Room> availableRooms = roomRepository.findAllByTypeAndOccupancyStatusAndHousekeepingStatus(res.getRoomType(), "VACANT", "CLEAN");
            
            // Extract just the Strings (e.g., ["101", "301"])
            List<String> availableRoomNumbers = availableRooms.stream()
                    .map(Room::getRoomNumber)
                    .collect(Collectors.toList());

            // Pass the list to Camunda for the dropdown menu
            variables.put("availableRoomList", availableRoomNumbers);
            
            // Pre-select the first one so the receptionist doesn't have to click!
            if (!availableRoomNumbers.isEmpty()) {
                variables.put("assignedRoomNumber", availableRoomNumbers.get(0)); 
            } else {
                variables.put("assignedRoomNumber", "NO ROOMS AVAILABLE");
            }
            
            System.out.println("✅ Found Check-In for " + res.getGuestName() + ". Found " + availableRoomNumbers.size() + " clean, vacant rooms.");
        } else {
            variables.put("reservationFound", false);
            System.out.println("❌ No reservation found for today under that Name/Code.");
        }

        return variables;
    }

    // ====================================================================================
    // WORKER 2: Execute Final Check-In (Linked to "Execute Check-In" Service Task)
    // ====================================================================================
    @JobWorker(type = "execute-checkin")
    public Map<String, Object> handleExecuteCheckIn(final ActivatedJob job) {
        
        String reservationCode = (String) job.getVariable("reservationCode");
        String assignedRoomNumber = (String) job.getVariable("assignedRoomNumber");

        System.out.println("🏨 Attempting final check-in for: " + reservationCode + " into Room: " + assignedRoomNumber);

        // Fetch BOTH records from the database
        Optional<Reservation> resOpt = reservationRepository.findByReservationCode(reservationCode);
        Optional<Room> roomOpt = roomRepository.findById(assignedRoomNumber);

        if (resOpt.isPresent() && roomOpt.isPresent()) {
            Reservation reservation = resOpt.get();
            Room room = roomOpt.get();

            // 1. Update the Reservation Status
            reservation.setStatus("CHECKED_IN");
            reservation.setAssignedRoomNumber(assignedRoomNumber);
            reservationRepository.save(reservation);

            // 2. Update the Physical Room Occupancy Status 
            room.setOccupancyStatus("OCCUPIED");
            // Note: housekeepingStatus remains 'CLEAN' until the next day's turnover
            roomRepository.save(room);

            System.out.println("✅ SUCCESS: Guest is officially in Room " + assignedRoomNumber);
            return Map.of("checkInComplete", true);
            
        } else {
            System.err.println("❌ ERROR: Missing DB records during final save!");
            return Map.of("checkInComplete", false);
        }
    }
}