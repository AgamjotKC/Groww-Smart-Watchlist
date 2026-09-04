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

    @PostMapping
    public ResponseEntity<Watchlist> createWatchlist(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String name,
            @RequestBody(required = false) Map<String, Object> body) {

        Long reqUserId = userId;
        String reqName = name;

        if (body != null) {
            if (reqUserId == null && body.containsKey("userId")) {
                reqUserId = Long.valueOf(body.get("userId").toString());
            }
            if (reqName == null && body.containsKey("name")) {
                reqName = body.get("name").toString();
            }
        }

        if (reqUserId == null) reqUserId = 1L;
        if (reqName == null) reqName = "My Watchlist";

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
    public ResponseEntity<Map<String, Object>> addStock(
            @PathVariable Long id,
            @RequestParam(required = false) String symbol,
            @RequestBody(required = false) Map<String, String> body) {

        String reqSymbol = symbol;
        if (body != null && body.containsKey("symbol")) {
            reqSymbol = body.get("symbol");
        }

        boolean success = watchlistService.addStock(id, reqSymbol);
        return ResponseEntity.ok(Map.of("success", success, "watchlistId", id, "symbol", reqSymbol != null ? reqSymbol.toUpperCase() : ""));
    }
}
