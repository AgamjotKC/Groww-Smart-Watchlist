package org.example.global.growwsmartwatchlist.controller;

import org.example.global.growwsmartwatchlist.model.Watchlist;
import org.example.global.growwsmartwatchlist.service.WatchlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/watchlists", "/api/watchlists"})
public class WatchlistController {

    @Autowired
    private WatchlistService watchlistService;

    public static class CreateWatchlistDto {
        private Long userId;
        private String name;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class AddStockDto {
        private String symbol;

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
    }

    @PostMapping
    public ResponseEntity<Watchlist> createWatchlist(@RequestBody(required = false) CreateWatchlistDto dto,
                                                     @RequestParam(required = false) Long userId,
                                                     @RequestParam(required = false) String name) {
        Long reqUserId = dto != null && dto.getUserId() != null ? dto.getUserId() : (userId != null ? userId : 1L);
        String reqName = dto != null && dto.getName() != null ? dto.getName() : (name != null ? name : "My Watchlist");

        Watchlist watchlist = watchlistService.createWatchlist(reqUserId, reqName);
        return ResponseEntity.ok(watchlist);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Watchlist> getWatchlist(@PathVariable Long id) {
        return watchlistService.getWatchlist(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/stocks")
    public ResponseEntity<Map<String, Object>> addStock(@PathVariable Long id,
                                                        @RequestBody(required = false) AddStockDto dto,
                                                        @RequestParam(required = false) String symbol) {
        String reqSymbol = dto != null && dto.getSymbol() != null ? dto.getSymbol() : symbol;

        boolean success = watchlistService.addStock(id, reqSymbol);
        return ResponseEntity.ok(Map.of("success", success, "watchlistId", id, "symbol", reqSymbol != null ? reqSymbol.toUpperCase() : ""));
    }
}
