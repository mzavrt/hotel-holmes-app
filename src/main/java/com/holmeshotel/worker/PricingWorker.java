package com.holmeshotel.worker;

import com.holmeshotel.repository.MarketRateRepository;
import com.holmeshotel.entity.MarketRate;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.annotation.JobWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PricingWorker {

    @Autowired
    private MarketRateRepository marketRateRepository;

    @JobWorker(type = "fetch-market-average")
    public Map<String, Object> handleFetchMarketAverage(final ActivatedJob job) {
        
        // 1. Get the room type requested by the guest
        String requestedType = (String) job.getVariable("roomType");

        // 2. Query the database for the market average
        // EXPERT TIP: We use .orElse() to provide a safe fallback price just in case 
        // the database doesn't have data for a specific room type!
        Double marketAverage = marketRateRepository.findByRoomType(requestedType)
                .map(MarketRate::getAveragePrice)
                .orElse(150.00); // Default safe fallback

        System.out.println("📈 [Pricing Data] Fetched Market Average for " + requestedType + ": €" + marketAverage);

        // 3. Return the variable to Camunda so the Gateway can evaluate the 5% rule
        return Map.of("marketAveragePrice", marketAverage);
    }
}