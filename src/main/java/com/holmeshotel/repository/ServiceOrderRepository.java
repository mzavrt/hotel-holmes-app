package com.holmeshotel.repository;

import com.holmeshotel.entity.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {
    
    Optional<ServiceOrder> findByTransactionId(String transactionId);
    
    // Finds all service orders for a specific room (Useful for checkout!)
    List<ServiceOrder> findByRoomNumber(Integer roomNumber);

    // Custom query to instantly get the total money owed by a specific room
    @Query("SELECT SUM(s.amountBilled) FROM ServiceOrder s WHERE s.roomNumber = :roomNumber AND s.status = 'BILLED_TO_ROOM'")
    Double calculateTotalUnpaidRoomCharges(@Param("roomNumber") Integer roomNumber);
}