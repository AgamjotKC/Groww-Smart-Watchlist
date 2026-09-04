package org.example.global.growwsmartwatchlist.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScoringEngineService {

    private static final double EPSILON = 1e-6;
    private static final double WEIGHT_Z_SCORE = 0.50;
    private static final double WEIGHT_VOLUME_RATIO = 0.30;
    private static final double WEIGHT_PROXIMITY = 0.20;

    public double calculateVolatilityZScore(List<Double> historicalPrices, double currentPrice) {
        if (historicalPrices == null || historicalPrices.size() < 2) {
            return 0.0;
        }
        double sum = 0.0;
        for (double p : historicalPrices) {
            sum += p;
        }
        double mean = sum / historicalPrices.size();

        double varianceSum = 0.0;
        for (double p : historicalPrices) {
            varianceSum += Math.pow(p - mean, 2);
        }
        double stdDev = Math.sqrt(varianceSum / historicalPrices.size());

        return Math.abs(currentPrice - mean) / (stdDev + EPSILON);
    }

    public double calculateVolumeSurgeRatio(long currentDayVolume, long averageDailyVolume20Day) {
        double baseline = Math.max((double) averageDailyVolume20Day, 1.0);
        double ratio = (double) currentDayVolume / baseline;
        return Math.min(8.0, Math.max(0.1, ratio));
    }

    public double calculateVolumeSurgeRatio(List<Long> historicalVolumes, long currentVolume) {
        if (historicalVolumes == null || historicalVolumes.isEmpty()) {
            return 1.0;
        }
        double sum = 0.0;
        for (long v : historicalVolumes) {
            sum += v;
        }
        double baselineAvg = sum / historicalVolumes.size();
        if (baselineAvg <= 0) return 1.0;

        double estimatedDailyBaseline = baselineAvg * Math.max(historicalVolumes.size(), 75);
        double ratio = (double) currentVolume / (historicalVolumes.size() > 10 ? estimatedDailyBaseline : baselineAvg);

        // Clamp volume surge ratio between 0.1x and 8.0x for realistic market bounds
        return Math.min(8.0, Math.max(0.1, ratio));
    }

    public double calculateLevelProximityScore(double currentPrice, double week52High, double week52Low) {
        if (week52High <= 0 || week52Low <= 0 || currentPrice <= 0) {
            return 0.0;
        }
        double distToHigh = Math.abs(currentPrice - week52High) / week52High;
        double distToLow = Math.abs(currentPrice - week52Low) / week52Low;
        double minDist = Math.min(distToHigh, distToLow);
        return Math.max(0.0, 1.0 - minDist);
    }

    public double calculateCompositeScore(List<Double> historicalPrices, double currentPrice,
                                           List<Long> historicalVolumes, long currentVolume,
                                           double week52High, double week52Low,
                                           boolean hasCatalyst) {
        double zScore = calculateVolatilityZScore(historicalPrices, currentPrice);
        double volumeRatio = calculateVolumeSurgeRatio(historicalVolumes, currentVolume);
        double proximityScore = calculateLevelProximityScore(currentPrice, week52High, week52Low);
        double catalystBoost = hasCatalyst ? 1.5 : 0.0;

        return (zScore * WEIGHT_Z_SCORE) + (volumeRatio * WEIGHT_VOLUME_RATIO) + (proximityScore * WEIGHT_PROXIMITY) + catalystBoost;
    }
}

