package org.example.global.growwsmartwatchlist.controller;

import org.example.global.growwsmartwatchlist.model.Watchlist;
import org.example.global.growwsmartwatchlist.service.WatchlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/watchlists")
public class WatchlistController {

    @Autowired
    private WatchlistService watchlistService;

    @PostMapping
    public ResponseEntity<Watchlist> createWatchlist(@RequestParam Long userId, @RequestParam String name) {
        Watchlist watchlist = watchlistService.createWatchlist(userId, name);
        return ResponseEntity.ok(watchlist);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Watchlist> getWatchlist(@PathVariable Long id) {
        return watchlistService.getWatchlist(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
