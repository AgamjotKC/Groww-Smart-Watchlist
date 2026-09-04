package org.example.global.growwsmartwatchlist.model;

import java.util.ArrayList;
import java.util.List;

public class DeltaResponse {
    private Long watchlistId;
    private String anchorMode;
    private List<ActiveMover> activeMovers = new ArrayList<>();
    private int quietCount;

    public DeltaResponse() {}

    public DeltaResponse(Long watchlistId, String anchorMode, List<ActiveMover> activeMovers, int quietCount) {
        this.watchlistId = watchlistId;
        this.anchorMode = anchorMode;
        this.activeMovers = activeMovers != null ? activeMovers : new ArrayList<>();
        this.quietCount = quietCount;
    }

    public Long getWatchlistId() { return watchlistId; }
    public void setWatchlistId(Long watchlistId) { this.watchlistId = watchlistId; }

    public String getAnchorMode() { return anchorMode; }
    public void setAnchorMode(String anchorMode) { this.anchorMode = anchorMode; }

    public List<ActiveMover> getActiveMovers() { return activeMovers; }
    public void setActiveMovers(List<ActiveMover> activeMovers) { this.activeMovers = activeMovers; }

    public int getQuietCount() { return quietCount; }
    public void setQuietCount(int quietCount) { this.quietCount = quietCount; }
}
