package org.example.global.growwsmartwatchlist.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScoringEngineService {

    private static final double EPSILON = 1e-6;

    public double calculateVolatilityZScore(List<Double> historicalPrices, double currentPrice) {
        if (historicalPrices == null || historicalPrices.isEmpty()) {
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
}
