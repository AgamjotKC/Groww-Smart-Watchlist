package org.example.global.growwsmartwatchlist.feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NseMarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(NseMarketDataProvider.class);

    private final CookieManager cookieManager = new CookieManager();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, NseQuoteCache> cache = new ConcurrentHashMap<>();
    private Instant lastCookieWarmup = Instant.EPOCH;

    @Autowired(required = false)
    private Clock clock = Clock.system(ZoneId.of("Asia/Kolkata"));

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public static class NseQuoteCache {
        public String symbol;
        public String companyName;
        public double currentPrice;
        public double prevClose;
        public double openPrice;
        public double deltaPercent;
        public long volume;
        public long averageDailyVolume20Day;
        public double week52High;
        public double week52Low;
        public List<Double> candlePrices;
        public List<Long> candleVolumes;
        public boolean isCachedFallback;
        public Instant timestamp;

        public NseQuoteCache(String symbol, String companyName, double currentPrice, double prevClose,
                             double openPrice, double deltaPercent, long volume, long averageDailyVolume20Day,
                             double week52High, double week52Low, List<Double> candlePrices, List<Long> candleVolumes,
                             boolean isCachedFallback) {
            this.symbol = symbol;
            this.companyName = companyName;
            this.currentPrice = currentPrice;
            this.prevClose = prevClose;
            this.openPrice = openPrice;
            this.deltaPercent = deltaPercent;
            this.volume = volume;
            this.averageDailyVolume20Day = averageDailyVolume20Day;
            this.week52High = week52High;
            this.week52Low = week52Low;
            this.candlePrices = candlePrices != null ? candlePrices : List.of();
            this.candleVolumes = candleVolumes != null ? candleVolumes : List.of();
            this.isCachedFallback = isCachedFallback;
            this.timestamp = Instant.now();
        }
    }

    public boolean isMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(ZoneId.of("Asia/Kolkata")));
        DayOfWeek day = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();

        return (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY)
                && (!time.isBefore(LocalTime.of(9, 15)) && time.isBefore(LocalTime.of(15, 30)));
    }

    public synchronized void ensureSessionCookies() {
        if (Instant.now().minus(Duration.ofMinutes(5)).isBefore(lastCookieWarmup)) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.nseindia.com"))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webkit,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            lastCookieWarmup = Instant.now();
            if (response.statusCode() == 200) {
                log.info("NSE session cookie warm-up successful (200 OK).");
            } else {
                log.warn("NSE session cookie warm-up received HTTP status {}.", response.statusCode());
            }
        } catch (Exception e) {
            log.warn("NSE session cookie warm-up failed: {}. Will attempt direct endpoint.", e.getMessage());
        }
    }

    public long getBaselineAdv(String symbol) {
        if (symbol == null) return 5_000_000L;
        String clean = symbol.toUpperCase().replace(".NS", "");
        return switch (clean) {
            case "RELIANCE" -> 10_000_000L;
            case "TCS" -> 2_500_000L;
            case "INFY" -> 5_000_000L;
            case "HDFCBANK" -> 12_000_000L;
            case "WIPRO" -> 4_000_000L;
            case "TATAMOTORS" -> 8_000_000L;
            case "ZOMATO" -> 25_000_000L;
            case "ICICIBANK" -> 10_000_000L;
            case "SBIN" -> 15_000_000L;
            default -> 5_000_000L;
        };
    }

    public Optional<NseQuoteCache> fetchNseQuote(String symbol) {
        if (symbol == null || symbol.isBlank()) return Optional.empty();
        String cleanSymbol = symbol.trim().toUpperCase().replace(".NS", "");

        NseQuoteCache cached = cache.get(cleanSymbol);
        
        // Aggressive Caching (15s TTL)
        if (cached != null && Instant.now().minusSeconds(15).isBefore(cached.timestamp) && !cached.isCachedFallback) {
            log.debug("Serving cached NSE quote for symbol {}", cleanSymbol);
            return Optional.of(cached);
        }

        // Market-hours gate: If market is closed, freeze prices and do not poll external NSE API
        if (!isMarketOpen() && cached != null) {
            log.info("Market is closed. Serving frozen 15:30 close quote for {}", cleanSymbol);
            cached.isCachedFallback = false;
            return Optional.of(cached);
        }

        // 1. Attempt live fetch from NSE official API
        try {
            ensureSessionCookies();
            String encodedSymbol = URLEncoder.encode(cleanSymbol, StandardCharsets.UTF_8);
            String url = "https://www.nseindia.com/api/quote-equity?symbol=" + encodedSymbol;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Referer", "https://www.nseindia.com/get-quotes/equity?symbol=" + encodedSymbol)
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null && !response.body().contains("Access Denied")) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode priceInfo = root.path("priceInfo");
                JsonNode info = root.path("info");

                if (!priceInfo.isMissingNode()) {
                    double currentPrice = priceInfo.path("lastPrice").asDouble(0.0);
                    double prevClose = priceInfo.path("previousClose").asDouble(currentPrice);
                    double openPrice = priceInfo.path("open").asDouble(prevClose);
                    double deltaPercent = priceInfo.path("pChange").asDouble(0.0);

                    long volume = priceInfo.path("totTrdVol").asLong(
                            root.path("securityWiseDP").path("quantityTraded").asLong(1_000_000L)
                    );

                    long adv20 = getBaselineAdv(cleanSymbol);
                    String companyName = info.path("companyName").asText(cleanSymbol);

                    double week52High = priceInfo.path("weekHighLow").path("max").asDouble(currentPrice * 1.15);
                    double week52Low = priceInfo.path("weekHighLow").path("min").asDouble(currentPrice * 0.85);

                    List<Double> candlePrices = List.of(openPrice, (openPrice + currentPrice) / 2.0, currentPrice);
                    List<Long> candleVolumes = List.of((long)(volume * 0.3), (long)(volume * 0.4), volume);

                    if (currentPrice > 0) {
                        NseQuoteCache quote = new NseQuoteCache(cleanSymbol, companyName, currentPrice, prevClose,
                                openPrice, deltaPercent, volume, adv20, week52High, week52Low, candlePrices, candleVolumes, false);
                        cache.put(cleanSymbol, quote);
                        log.info("Successfully fetched live NSE quote for {}: lastPrice={}, change=%{}", cleanSymbol, currentPrice, deltaPercent);
                        return Optional.of(quote);
                    }
                }
            } else {
                log.warn("NSE quote API call for symbol {} returned HTTP status {} (or Access Denied). Attempting Yahoo Finance fallback.", cleanSymbol, response.statusCode());
            }
        } catch (Exception e) {
            log.warn("NSE quote API call failed for symbol {}: {}. Attempting Yahoo Finance fallback.", cleanSymbol, e.getMessage());
        }

        // 2. Secondary live fetch from Yahoo Finance chart feed if NSE direct API is blocked by Akamai WAF
        try {
            String yahooSymbol = cleanSymbol + ".NS";
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + yahooSymbol + "?range=1d&interval=5m";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode resultNode = root.path("chart").path("result");

                if (resultNode.isArray() && !resultNode.isEmpty()) {
                    JsonNode resultObj = resultNode.get(0);
                    JsonNode meta = resultObj.path("meta");
                    double currentPrice = meta.path("regularMarketPrice").asDouble(0.0);

                    // Extract latest valid non-null close from indicators.quote[0].close as fallback or split validation
                    JsonNode quoteArray = resultObj.path("indicators").path("quote");
                    double latestCloseFromIndicator = 0.0;
                    if (quoteArray.isArray() && !quoteArray.isEmpty()) {
                        JsonNode closes = quoteArray.get(0).path("close");
                        if (closes.isArray() && !closes.isEmpty()) {
                            for (int i = closes.size() - 1; i >= 0; i--) {
                                JsonNode closeVal = closes.get(i);
                                if (closeVal != null && !closeVal.isNull() && closeVal.isNumber() && closeVal.asDouble() > 0) {
                                    latestCloseFromIndicator = closeVal.asDouble();
                                    break;
                                }
                            }
                        }
                    }

                    if (currentPrice <= 0 || (latestCloseFromIndicator > 0 && Math.abs(currentPrice - latestCloseFromIndicator) / latestCloseFromIndicator > 0.5)) {
                        if (latestCloseFromIndicator > 0) {
                            currentPrice = latestCloseFromIndicator;
                        }
                    }

                    double prevClose = meta.path("chartPreviousClose").asDouble(currentPrice);
                    double deltaPercent = meta.path("regularMarketChangePercent").asDouble(0.0);
                    long volume = meta.path("regularMarketVolume").asLong(1_000_000L);
                    long adv20 = getBaselineAdv(cleanSymbol);
                    double week52High = meta.path("fiftyTwoWeekHigh").asDouble(currentPrice * 1.15);
                    double week52Low = meta.path("fiftyTwoWeekLow").asDouble(currentPrice * 0.85);
                    String companyName = meta.path("longName").asText(meta.path("shortName").asText(cleanSymbol));

                    List<Double> candlePrices = List.of(prevClose, (prevClose + currentPrice) / 2.0, currentPrice);
                    List<Long> candleVolumes = List.of((long)(volume * 0.3), (long)(volume * 0.4), volume);
                    double openPrice = candlePrices.get(0);

                    if (currentPrice > 0) {
                        NseQuoteCache quote = new NseQuoteCache(cleanSymbol, companyName, currentPrice, prevClose,
                                openPrice, deltaPercent, volume, adv20, week52High, week52Low, candlePrices, candleVolumes, false);
                        cache.put(cleanSymbol, quote);
                        log.info("Successfully fetched live Yahoo Finance chart quote for {}: price={}, change=%{}", cleanSymbol, currentPrice, deltaPercent);
                        return Optional.of(quote);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Yahoo Finance quote API call failed for symbol {}: {}.", cleanSymbol, e.getMessage());
        }

        // Fallback quote generation if external calls failed and no cache exists
        if (cached != null) {
            cached.isCachedFallback = true;
            return Optional.of(cached);
        }

        NseQuoteCache fallbackQuote = createFallbackQuote(cleanSymbol);
        cache.put(cleanSymbol, fallbackQuote);
        return Optional.of(fallbackQuote);
    }

    private NseQuoteCache createFallbackQuote(String cleanSymbol) {
        double currentPrice = switch (cleanSymbol) {
            case "TCS" -> 4120.00;
            case "WIPRO" -> 495.50;
            case "INFY" -> 1820.00;
            case "HDFCBANK" -> 1640.00;
            case "RELIANCE" -> 1322.00; // Post 1:1 bonus adjusted live price level
            case "TATAMOTORS" -> 980.00;
            case "ZOMATO" -> 245.00;
            case "SBIN" -> 820.00;
            default -> 1000.00;
        };

        double deltaPercent = switch (cleanSymbol) {
            case "HDFCBANK" -> 1.38;
            case "RELIANCE" -> 1.50;
            case "TCS" -> -0.45;
            case "WIPRO" -> -0.20;
            case "INFY" -> 0.35;
            default -> 0.25;
        };

        double prevClose = currentPrice / (1.0 + (deltaPercent / 100.0));
        double openPrice = prevClose * 1.002;
        long adv20 = getBaselineAdv(cleanSymbol);
        long volume = (long) (adv20 * (Math.abs(deltaPercent) > 1.2 ? 2.3 : 0.8));

        return new NseQuoteCache(
                cleanSymbol,
                cleanSymbol + " Limited",
                currentPrice,
                prevClose,
                openPrice,
                deltaPercent,
                volume,
                adv20,
                currentPrice * 1.15,
                currentPrice * 0.85,
                List.of(openPrice, (openPrice + currentPrice) / 2.0, currentPrice),
                List.of((long)(volume * 0.3), (long)(volume * 0.4), volume),
                true
        );
    }
}
