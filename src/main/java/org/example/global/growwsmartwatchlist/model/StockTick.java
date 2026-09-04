package org.example.global.growwsmartwatchlist.model;

import java.time.Instant;

public class StockTick {
    private String symbol;
    private double price;
    private long volume;
    private Instant timestamp;

    public StockTick() {}

    public StockTick(String symbol, double price, long volume, Instant timestamp) {
        this.symbol = symbol;
        this.price = price;
        this.volume = volume;
        this.timestamp = timestamp;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public long getVolume() { return volume; }
    public void setVolume(long volume) { this.volume = volume; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
