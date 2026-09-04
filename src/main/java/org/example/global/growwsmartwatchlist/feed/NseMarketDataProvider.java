package org.example.global.growwsmartwatchlist.feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.global.growwsmartwatchlist.model.StockTick;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class NseMarketDataProvider {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, NseQuoteCache> cache = new HashMap<>();

    public static class NseQuoteCache {
        public String symbol;
        public String companyName;
        public double currentPrice;
        public double prevClose;
        public double deltaPercent;
        public long volume;
        public double week52High;
        public double week52Low;
        public Instant timestamp;

        public NseQuoteCache(String symbol, String companyName, double currentPrice, double prevClose,
                             double deltaPercent, long volume, double week52High, double week52Low) {
            this.symbol = symbol;
            this.companyName = companyName;
            this.currentPrice = currentPrice;
            this.prevClose = prevClose;
            this.deltaPercent = deltaPercent;
            this.volume = volume;
            this.week52High = week52High;
            this.week52Low = week52Low;
            this.timestamp = Instant.now();
        }
    }

    public Optional<NseQuoteCache> fetchNseQuote(String symbol) {
        if (symbol == null || symbol.isBlank()) return Optional.empty();
        String cleanSymbol = symbol.trim().toUpperCase();

        NseQuoteCache cached = cache.get(cleanSymbol);
        if (cached != null && Instant.now().minusSeconds(5).isBefore(cached.timestamp)) {
            return Optional.of(cached);
        }

        try {
            String yahooSymbol = cleanSymbol.endsWith(".NS") ? cleanSymbol : cleanSymbol + ".NS";
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + yahooSymbol + "?range=1d&interval=5m";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode resultNode = root.path("chart").path("result");

                if (resultNode.isArray() && !resultNode.isEmpty()) {
                    JsonNode meta = resultNode.get(0).path("meta");

                    double currentPrice = meta.path("regularMarketPrice").asDouble(0.0);
                    double prevClose = meta.path("chartPreviousClose").asDouble(currentPrice);
                    double deltaPercent = meta.path("regularMarketChangePercent").asDouble(0.0);
                    long volume = meta.path("regularMarketVolume").asLong(100000);
                    double week52High = meta.path("fiftyTwoWeekHigh").asDouble(currentPrice * 1.2);
                    double week52Low = meta.path("fiftyTwoWeekLow").asDouble(currentPrice * 0.8);
                    String companyName = meta.path("longName").asText(meta.path("shortName").asText(cleanSymbol));

                    if (currentPrice > 0) {
                        NseQuoteCache quote = new NseQuoteCache(cleanSymbol, companyName, currentPrice, prevClose,
                                deltaPercent, volume, week52High, week52Low);
                        cache.put(cleanSymbol, quote);
                        return Optional.of(quote);
                    }
                }
            }
        } catch (Exception e) {
            // Graceful fallback to cached or simulated quote
        }

        return Optional.ofNullable(cache.get(cleanSymbol));
    }
}
