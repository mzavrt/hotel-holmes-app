package com.holmeshotel.repository;

import com.holmeshotel.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    // Custom query to count overlapping bookings
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.roomType = :roomType AND r.status IN ('CONFIRMED', 'CHECKED_IN', 'HELD') AND r.checkInDate < :checkOutDate AND r.checkOutDate > :checkInDate")
    long countOverlappingReservations(
        @Param("roomType") String roomType, 
        @Param("checkInDate") LocalDate checkInDate, 
        @Param("checkOutDate") LocalDate checkOutDate
    );
    Optional<Reservation> findByReservationCode(String reservationCode);
    List<Reservation> findByGuestNameContainingIgnoreCase(String guestName);
}