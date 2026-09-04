package org.example.global.growwsmartwatchlist;

import org.example.global.growwsmartwatchlist.service.ScoringEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ScoringEngineServiceTest {

    @Autowired
    private ScoringEngineService scoringEngineService;

    @Test
    void testVolatilityZScoreCalculation() {
        List<Double> historicalPrices = List.of(100.0, 102.0, 98.0, 101.0, 99.0);
        double currentPrice = 115.0;

        double zScore = scoringEngineService.calculateVolatilityZScore(historicalPrices, currentPrice);
        assertEquals(9.6, zScore, 0.5);
    }
}
