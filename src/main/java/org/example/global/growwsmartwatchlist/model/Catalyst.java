package org.example.global.growwsmartwatchlist.model;

import java.time.Instant;

public class Catalyst {
    private String symbol;
    private String eventType;
    private String title;
    private String newsUrl;
    private Instant timestamp;

    public Catalyst() {}

    public Catalyst(String symbol, String eventType, String title, String newsUrl, Instant timestamp) {
        this.symbol = symbol;
        this.eventType = eventType;
        this.title = title;
        this.newsUrl = newsUrl;
        this.timestamp = timestamp;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getNewsUrl() { return newsUrl; }
    public void setNewsUrl(String newsUrl) { this.newsUrl = newsUrl; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}

