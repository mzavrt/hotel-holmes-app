package com.holmeshotel.repository;

import com.holmeshotel.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {
    
    // Finds one room matching the exact type, occupancy, and housekeeping state
    Optional<Room> findFirstByTypeAndOccupancyStatusAndHousekeepingStatus(String type, String occupancyStatus, String housekeepingStatus);

    // Finds all rooms matching the exact type, occupancy, and housekeeping state
    List<Room> findAllByTypeAndOccupancyStatusAndHousekeepingStatus(String type, String occupancyStatus, String housekeepingStatus);
    
    // Added to find total inventory regardless of current status
    long countByType(String type); 
}