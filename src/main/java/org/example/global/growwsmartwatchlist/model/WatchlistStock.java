package org.example.global.growwsmartwatchlist.model;

public class WatchlistStock {
    private Long id;
    private Long watchlistId;
    private String symbol;

    public WatchlistStock() {}

    public WatchlistStock(Long id, Long watchlistId, String symbol) {
        this.id = id;
        this.watchlistId = watchlistId;
        this.symbol = symbol;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getWatchlistId() { return watchlistId; }
    public void setWatchlistId(Long watchlistId) { this.watchlistId = watchlistId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
}
