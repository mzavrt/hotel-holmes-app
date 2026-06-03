package com.holmeshotel.repository;

import com.holmeshotel.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {
    Optional<Room> findFirstByTypeAndStatus(String type, String status);
    
    // Added to find total inventory regardless of current status
    long countByType(String type); 
}