package org.example.global.growwsmartwatchlist.service;

import org.example.global.growwsmartwatchlist.model.Catalyst;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
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

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private static class CachedCatalyst {
        Catalyst catalyst;
        Instant timestamp;

        CachedCatalyst(Catalyst catalyst) {
            this.catalyst = catalyst;
            this.timestamp = Instant.now();
        }
    }

    private final Map<String, CachedCatalyst> cache = new ConcurrentHashMap<>();

    public boolean hasCatalyst(String symbol) {
        return getLatestCatalyst(symbol).isPresent();
    }

    public Optional<Catalyst> getLatestCatalyst(String symbol) {
        if (symbol == null || symbol.isBlank()) return Optional.empty();
        String cleanSymbol = symbol.trim().toUpperCase().replace(".NS", "");

        CachedCatalyst cached = cache.get(cleanSymbol);
        if (cached != null && Instant.now().minusSeconds(120).isBefore(cached.timestamp)) {
            return Optional.ofNullable(cached.catalyst);
        }

        try {
            String queryUrl = "https://news.google.com/rss/search?q=" + cleanSymbol + "+NSE+stock+news&hl=en-IN&gl=IN&ceid=IN:en";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(queryUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(response.body().getBytes(StandardCharsets.UTF_8)));

                NodeList items = doc.getElementsByTagName("item");
                if (items.getLength() > 0) {
                    Element firstItem = (Element) items.item(0);
                    String rawTitle = getElementText(firstItem, "title");
                    String link = getElementText(firstItem, "link");

                    if (rawTitle != null && !rawTitle.isBlank()) {
                        String eventType = determineEventType(rawTitle);
                        String cleanTitle = cleanHeadline(rawTitle);
                        String targetUrl = (link != null && !link.isBlank()) ? link : "https://www.google.com/finance/quote/" + cleanSymbol + ":NSE";

                        Catalyst catalyst = new Catalyst(cleanSymbol, eventType, cleanTitle, targetUrl, Instant.now());
                        cache.put(cleanSymbol, new CachedCatalyst(catalyst));
                        return Optional.of(catalyst);
                    }
                }
            }
        } catch (Exception e) {
            // Graceful fallback to cached or NSE default
        }

        if (cached != null) {
            return Optional.ofNullable(cached.catalyst);
        }

        String nseUrl = "https://www.google.com/finance/quote/" + cleanSymbol + ":NSE";
        Catalyst defaultCat = new Catalyst(cleanSymbol, "Filing", "Official Corporate Filings & Announcements on NSE", nseUrl, Instant.now());
        cache.put(cleanSymbol, new CachedCatalyst(defaultCat));
        return Optional.of(defaultCat);
    }

    private String getElementText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent();
        }
        return "";
    }

    private String determineEventType(String title) {
        String lower = title.toLowerCase();
        if (lower.contains("dividend") || lower.contains("ex-dividend") || lower.contains("payout")) {
            return "Dividend";
        } else if (lower.contains("earning") || lower.contains("profit") || lower.contains("revenue") || lower.contains("q1") || lower.contains("q2") || lower.contains("q3") || lower.contains("q4") || lower.contains("result")) {
            return "Earnings";
        } else if (lower.contains("board") || lower.contains("meeting") || lower.contains("ceo") || lower.contains("demerger") || lower.contains("acquisition")) {
            return "Board Meeting";
        } else {
            return "Filing";
        }
    }

    private String cleanHeadline(String title) {
        if (title == null) return "";
        int lastDash = title.lastIndexOf(" - ");
        if (lastDash > 20) {
            return title.substring(0, lastDash).trim();
        }
        return title.trim();
    }
}

