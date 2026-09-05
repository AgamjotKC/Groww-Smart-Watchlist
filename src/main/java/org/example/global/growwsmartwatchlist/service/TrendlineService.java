package org.example.global.growwsmartwatchlist.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TrendlineService {

    private static final Logger log = LoggerFactory.getLogger(TrendlineService.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Optional<String>> crossoverCache = new ConcurrentHashMap<>();

    public Optional<String> detectCrossover(String symbol) {
        if (symbol == null || symbol.isBlank()) return Optional.empty();
        String cleanSymbol = symbol.trim().toUpperCase().replace(".NS", "");

        if (crossoverCache.containsKey(cleanSymbol)) {
            return crossoverCache.get(cleanSymbol);
        }

        Optional<String> result = computeCrossover(cleanSymbol);
        crossoverCache.put(cleanSymbol, result);
        return result;
    }

    private Optional<String> computeCrossover(String cleanSymbol) {
        try {
            String yahooSymbol = switch (cleanSymbol) {
                case "LTIM" -> "LTI.NS";
                case "M&M" -> "M%26M.NS";
                default -> cleanSymbol + ".NS";
            };
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + yahooSymbol + "?range=3mo&interval=1d";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode resultNode = root.path("chart").path("result");

                if (resultNode.isArray() && !resultNode.isEmpty()) {
                    JsonNode quoteObj = resultNode.get(0).path("indicators").path("quote");
                    if (quoteObj.isArray() && !quoteObj.isEmpty()) {
                        JsonNode closes = quoteObj.get(0).path("close");
                        List<Double> dailyCloses = new ArrayList<>();
                        if (closes.isArray()) {
                            for (JsonNode c : closes) {
                                if (c != null && !c.isNull() && c.isNumber() && c.asDouble() > 0) {
                                    dailyCloses.add(c.asDouble());
                                }
                            }
                        }

                        if (dailyCloses.size() >= 53) {
                            int n = dailyCloses.size();
                            double[] sma20 = new double[n];
                            double[] sma50 = new double[n];

                            for (int i = 19; i < n; i++) {
                                double sum20 = 0.0;
                                for (int j = i - 19; j <= i; j++) {
                                    sum20 += dailyCloses.get(j);
                                }
                                sma20[i] = sum20 / 20.0;
                            }

                            for (int i = 49; i < n; i++) {
                                double sum50 = 0.0;
                                for (int j = i - 49; j <= i; j++) {
                                    sum50 += dailyCloses.get(j);
                                }
                                sma50[i] = sum50 / 50.0;
                            }

                            int latestIdx = n - 1;
                            double currentSma20 = sma20[latestIdx];
                            double currentSma50 = sma50[latestIdx];

                            // Golden Cross: SMA20 > SMA50 now AND SMA20 <= SMA50 in any of last 1-3 trading days
                            if (currentSma20 > currentSma50) {
                                boolean wasBelow = false;
                                for (int k = 1; k <= 3; k++) {
                                    int checkIdx = latestIdx - k;
                                    if (checkIdx >= 49 && sma20[checkIdx] <= sma50[checkIdx]) {
                                        wasBelow = true;
                                        break;
                                    }
                                }
                                if (wasBelow) {
                                    log.info("[TRENDLINE] Golden Cross detected for symbol {} (SMA20={}, SMA50={})", cleanSymbol, currentSma20, currentSma50);
                                    return Optional.of("Golden Cross");
                                }
                            }

                            // Death Cross: SMA20 < SMA50 now AND SMA20 >= SMA50 in any of last 1-3 trading days
                            if (currentSma20 < currentSma50) {
                                boolean wasAbove = false;
                                for (int k = 1; k <= 3; k++) {
                                    int checkIdx = latestIdx - k;
                                    if (checkIdx >= 49 && sma20[checkIdx] >= sma50[checkIdx]) {
                                        wasAbove = true;
                                        break;
                                    }
                                }
                                if (wasAbove) {
                                    log.info("[TRENDLINE] Death Cross detected for symbol {} (SMA20={}, SMA50={})", cleanSymbol, currentSma20, currentSma50);
                                    return Optional.of("Death Cross");
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[TRENDLINE] Failed to compute daily trendline crossover for symbol {}: {}", cleanSymbol, e.getMessage());
        }
        return Optional.empty();
    }
}
