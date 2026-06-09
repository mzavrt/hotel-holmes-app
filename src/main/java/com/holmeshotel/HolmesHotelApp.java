package com.holmeshotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class HolmesHotelApp {

	public static void main(String[] args) {
		SpringApplication.run(HolmesHotelApp.class, args);
	}

	 @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
