package org.example.global.growwsmartwatchlist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GrowwSmartWatchlistApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrowwSmartWatchlistApplication.class, args);
    }
}
