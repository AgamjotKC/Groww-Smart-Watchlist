package org.example.global.growwsmartwatchlist.model;

import java.util.ArrayList;
import java.util.List;

public class DeltaResponse {
    private Long watchlistId;
    private String anchorMode;
    private String lastSeenAt;
    private List<ActiveMover> activeMovers = new ArrayList<>();
    private int quietCount;
    private List<ActiveMover> quietStocks = new ArrayList<>();

    public DeltaResponse() {}

    public DeltaResponse(Long watchlistId, String anchorMode, List<ActiveMover> activeMovers, int quietCount, List<ActiveMover> quietStocks) {
        this.watchlistId = watchlistId;
        this.anchorMode = anchorMode;
        this.activeMovers = activeMovers != null ? activeMovers : new ArrayList<>();
        this.quietCount = quietCount;
        this.quietStocks = quietStocks != null ? quietStocks : new ArrayList<>();
    }

    public Long getWatchlistId() { return watchlistId; }
    public void setWatchlistId(Long watchlistId) { this.watchlistId = watchlistId; }

    public String getAnchorMode() { return anchorMode; }
    public void setAnchorMode(String anchorMode) { this.anchorMode = anchorMode; }

    public String getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(String lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public List<ActiveMover> getActiveMovers() { return activeMovers; }
    public void setActiveMovers(List<ActiveMover> activeMovers) { this.activeMovers = activeMovers; }

    public int getQuietCount() { return quietCount; }
    public void setQuietCount(int quietCount) { this.quietCount = quietCount; }

    public List<ActiveMover> getQuietStocks() { return quietStocks; }
    public void setQuietStocks(List<ActiveMover> quietStocks) { this.quietStocks = quietStocks; }
}
