package org.example.global.growwsmartwatchlist.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Watchlist {
    private Long id;
    private Long userId;
    private String name;
    private LocalDateTime lastSeenAt;
    private List<String> symbols = new ArrayList<>();

    public Watchlist() {}

    public Watchlist(Long id, Long userId, String name, LocalDateTime lastSeenAt, List<String> symbols) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.lastSeenAt = lastSeenAt;
        this.symbols = symbols != null ? symbols : new ArrayList<>();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public List<String> getSymbols() { return symbols; }
    public void setSymbols(List<String> symbols) { this.symbols = symbols; }
}
