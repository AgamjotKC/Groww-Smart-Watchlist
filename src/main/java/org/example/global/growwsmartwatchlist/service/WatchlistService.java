package org.example.global.growwsmartwatchlist.service;

import org.example.global.growwsmartwatchlist.model.Watchlist;
import org.example.global.growwsmartwatchlist.model.WatchlistStock;
import org.example.global.growwsmartwatchlist.repository.WatchlistRepository;
import org.example.global.growwsmartwatchlist.repository.WatchlistStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class WatchlistService {

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private WatchlistStockRepository watchlistStockRepository;

    public Watchlist createWatchlist(Long userId, String name) {
        if (userId == null || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID and Watchlist name must be provided");
        }
        Watchlist watchlist = new Watchlist();
        watchlist.setUserId(userId);
        watchlist.setName(name.trim());
        watchlist.setLastSeenAt(LocalDateTime.now());
        return watchlistRepository.save(watchlist);
    }

    public Optional<Watchlist> getWatchlist(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        Optional<Watchlist> watchlistOpt = watchlistRepository.findById(id);
        if (watchlistOpt.isPresent()) {
            Watchlist watchlist = watchlistOpt.get();
            watchlist.setLastSeenAt(LocalDateTime.now());
            watchlistRepository.save(watchlist);
        }
        return watchlistOpt;
    }

    public boolean addStock(Long watchlistId, String symbol) {
        if (watchlistId == null || symbol == null || symbol.trim().isEmpty()) {
            return false;
        }
        String formattedSymbol = symbol.toUpperCase().trim();
        if (watchlistStockRepository.existsByWatchlistIdAndSymbol(watchlistId, formattedSymbol)) {
            return false;
        }
        WatchlistStock stock = new WatchlistStock(watchlistId, formattedSymbol, LocalDateTime.now());
        watchlistStockRepository.save(stock);
        return true;
    }

    @Transactional
    public boolean removeStock(Long watchlistId, String symbol) {
        if (watchlistId == null || symbol == null || symbol.trim().isEmpty()) {
            return false;
        }
        String formattedSymbol = symbol.toUpperCase().trim();
        Optional<WatchlistStock> stockOpt = watchlistStockRepository.findByWatchlistIdAndSymbol(watchlistId, formattedSymbol);
        if (stockOpt.isPresent()) {
            watchlistStockRepository.delete(stockOpt.get());
            return true;
        }
        return false;
    }
}
