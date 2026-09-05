package org.example.global.growwsmartwatchlist;

import org.example.global.growwsmartwatchlist.service.ScoringEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void testCompositeScoreCatalystBoost() {
        List<Double> histPrices = List.of(100.0, 101.0, 99.0);
        List<Long> histVolumes = List.of(10000L, 10000L);

        double scoreWithoutCatalyst = scoringEngineService.calculateCompositeScore(histPrices, 100.0, histVolumes, 10000L, 120.0, 80.0, false);
        double scoreWithCatalyst = scoringEngineService.calculateCompositeScore(histPrices, 100.0, histVolumes, 10000L, 120.0, 80.0, true);

        assertTrue(scoreWithCatalyst > scoreWithoutCatalyst);
    }

    @Test
    void testCompositeScore52WeekHighProximityBoost() {
        List<Double> histPrices = List.of(100.0, 101.0, 99.0);
        List<Long> histVolumes = List.of(10000L, 10000L);

        double scoreNearHigh = scoringEngineService.calculateCompositeScore(histPrices, 119.0, histVolumes, 10000L, 120.0, 80.0, false);
        double scoreMidRange = scoringEngineService.calculateCompositeScore(histPrices, 100.0, histVolumes, 10000L, 120.0, 80.0, false);

        assertTrue(scoreNearHigh > scoreMidRange);
    }

    @Test
    void testNullHistoricalVolumesHandling() {
        double surgeRatio = scoringEngineService.calculateVolumeSurgeRatio((List<Long>) null, 50000L);
        assertEquals(1.0, surgeRatio);
    }

    @Test
    void testZeroAdvHandling() {
        List<Long> zeroVolumes = List.of(0L, 0L, 0L);
        double surgeRatio = scoringEngineService.calculateVolumeSurgeRatio(zeroVolumes, 50000L);
        assertEquals(1.0, surgeRatio);
    }
}
