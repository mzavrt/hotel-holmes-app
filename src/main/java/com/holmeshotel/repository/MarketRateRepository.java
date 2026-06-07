package com.holmeshotel.repository;

import com.holmeshotel.entity.MarketRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketRateRepository extends JpaRepository<MarketRate, Long> {
    
    // Spring Data JPA automatically writes the SQL for this based on the method name!
    Optional<MarketRate> findByRoomType(String roomType);
}