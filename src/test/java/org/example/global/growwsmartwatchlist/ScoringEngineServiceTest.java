package org.example.global.growwsmartwatchlist;

import org.example.global.growwsmartwatchlist.service.ScoringEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ScoringEngineServiceTest {

    @Autowired
    private ScoringEngineService scoringEngineService;

    @Test
    void testVolatilityZScoreCalculation() {
        List<Double> historicalPrices = List.of(100.0, 102.0, 98.0, 101.0, 99.0);
        double currentPrice = 115.0;

        double zScore = scoringEngineService.calculateVolatilityZScore(historicalPrices, currentPrice);
        assertEquals(10.6, zScore, 0.5);
    }

    @Test
    void testZeroVolatilityZeroDivideEdgeCase() {
        List<Double> zeroVolPrices = List.of(100.0, 100.0, 100.0, 100.0);
        double currentPrice = 105.0;

        double zScore = scoringEngineService.calculateVolatilityZScore(zeroVolPrices, currentPrice);
        assertTrue(Double.isFinite(zScore));
        assertTrue(zScore > 0);
    }

    @Test
    void testVolumeSurgeRatioCalculation() {
        List<Long> historicalVolumes = List.of(100000L, 120000L, 110000L, 90000L);
        long currentVolume = 315000L;

        double surgeRatio = scoringEngineService.calculateVolumeSurgeRatio(historicalVolumes, currentVolume);
        assertEquals(3.0, surgeRatio, 0.1);
    }

    @Test
    void testBrandNewListingNoHistoricalWindow() {
        List<Double> emptyHistory = List.of();
        double currentPrice = 500.0;

        double zScore = scoringEngineService.calculateVolatilityZScore(emptyHistory, currentPrice);
        assertEquals(0.0, zScore);

        double compositeScore = scoringEngineService.calculateCompositeScore(emptyHistory, currentPrice, List.of(), 10000L, 600.0, 400.0, false);
        assertTrue(Double.isFinite(compositeScore));
    }
}
