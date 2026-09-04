package org.example.global.growwsmartwatchlist.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.global.growwsmartwatchlist.model.Catalyst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CatalystService {

    private static final Logger log = LoggerFactory.getLogger(CatalystService.class);

    private final CookieManager cookieManager = new CookieManager();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static class CachedCatalyst {
        Catalyst catalyst;
        Instant timestamp;

        CachedCatalyst(Catalyst catalyst) {
            this.catalyst = catalyst;
            this.timestamp = Instant.now();
        }
    }

    private final Map<String, CachedCatalyst> cache = new ConcurrentHashMap<>();
    private Instant lastCookieWarmup = Instant.EPOCH;

    public boolean hasCatalyst(String symbol) {
        return getLatestCatalyst(symbol).isPresent();
    }

    private synchronized void ensureSessionCookies() {
        if (Instant.now().minus(Duration.ofMinutes(5)).isBefore(lastCookieWarmup)) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.nseindia.com"))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            lastCookieWarmup = Instant.now();
        } catch (Exception e) {
            log.warn("Cookie warm-up for corporate announcements failed: {}", e.getMessage());
        }
    }

    public Optional<Catalyst> getLatestCatalyst(String symbol) {
        if (symbol == null || symbol.isBlank()) return Optional.empty();
        String cleanSymbol = symbol.trim().toUpperCase().replace(".NS", "");

        CachedCatalyst cached = cache.get(cleanSymbol);
        if (cached != null && Instant.now().minusSeconds(300).isBefore(cached.timestamp)) {
            return Optional.ofNullable(cached.catalyst);
        }

        try {
            ensureSessionCookies();
            String encodedSymbol = URLEncoder.encode(cleanSymbol, StandardCharsets.UTF_8);
            String url = "https://www.nseindia.com/api/corporate-announcements?index=equities&symbol=" + encodedSymbol;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Referer", "https://www.nseindia.com/companies-listing/corporate-filings-announcements")
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null && !response.body().contains("Access Denied")) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.isArray() && !root.isEmpty()) {
                    JsonNode announcement = root.get(0);
                    
                    // Directly map response category/subject fields without free-text keyword guessing
                    String desc = announcement.path("desc").asText(announcement.path("subject").asText(""));
                    String attchmntText = announcement.path("attchmntText").asText("");

                    String eventType = normalizeCategory(desc, attchmntText);

                    String attchmnt = announcement.path("attchmntFile").asText("");
                    String newsUrl = (!attchmnt.isBlank()) 
                            ? "https://nsearchives.nseindia.com/corporate/" + attchmnt 
                            : "https://www.nseindia.com/get-quotes/equity?symbol=" + cleanSymbol + "#announcements";

                    Catalyst catalyst = new Catalyst(cleanSymbol, eventType, desc.isBlank() ? "Official Corporate Disclosure" : desc, newsUrl, Instant.now());
                    cache.put(cleanSymbol, new CachedCatalyst(catalyst));
                    log.info("Fetched live NSE announcement for {}: eventType='{}', desc='{}'", cleanSymbol, eventType, desc);
                    return Optional.of(catalyst);
                }
            } else {
                log.warn("NSE corporate-announcements call for {} returned HTTP status {}. Serving cached/fallback catalyst.", cleanSymbol, response.statusCode());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch NSE corporate-announcements for {}: {}. Serving cached/fallback catalyst.", cleanSymbol, e.getMessage());
        }

        if (cached != null) {
            return Optional.ofNullable(cached.catalyst);
        }

        // Default NSE corporate announcement fallback
        String fallbackUrl = "https://www.nseindia.com/get-quotes/equity?symbol=" + cleanSymbol + "#announcements";
        Catalyst defaultCat = new Catalyst(cleanSymbol, "Corporate Update", "Official Corporate Filings & Announcements on NSE", fallbackUrl, Instant.now());
        cache.put(cleanSymbol, new CachedCatalyst(defaultCat));
        return Optional.of(defaultCat);
    }

    public String normalizeCategory(String desc, String text) {
        String blob = ((desc != null ? desc : "") + " " + (text != null ? text : "")).toLowerCase();
        
        if (blob.contains("dividend") || blob.contains("distribution")) return "Dividend";
        if (blob.contains("result") || blob.contains("financial") || blob.contains("profit") || blob.contains("earning") || blob.contains("earnings")) return "Earnings";
        if (blob.contains("board meeting") || blob.contains("meeting")) return "Board Meeting";
        if (blob.contains("acquisition") || blob.contains("partner") || blob.contains("merger") || blob.contains("demerger") || blob.contains("joint venture")) return "M&A / Partnership";
        if (blob.contains("order") || blob.contains("contract") || blob.contains("award") || blob.contains("agreement")) return "Contract Win";
        if (blob.contains("resignation") || blob.contains("appointment") || blob.contains("director")) return "Management";
        
        return "Corporate Update";
    }
}
