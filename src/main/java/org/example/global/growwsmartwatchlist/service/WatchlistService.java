package org.example.global.growwsmartwatchlist.service;

import jakarta.annotation.PostConstruct;
import org.example.global.growwsmartwatchlist.model.Watchlist;
import org.example.global.growwsmartwatchlist.model.WatchlistStock;
import org.example.global.growwsmartwatchlist.repository.WatchlistRepository;
import org.example.global.growwsmartwatchlist.repository.WatchlistStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WatchlistService {

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private WatchlistStockRepository watchlistStockRepository;

    @PostConstruct
    public void initDefaultWatchlist() {
        if (watchlistRepository.count() == 0) {
            Watchlist defaultList = createWatchlist(1L, "Primary Catch-Up List");
            List<String> seedStocks = List.of("RELIANCE", "TCS", "INFY", "HDFCBANK", "WIPRO", "TATAMOTORS", "ZOMATO", "SBIN");
            for (String sym : seedStocks) {
                addStock(defaultList.getId(), sym);
            }
        }
    }

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
        return watchlistRepository.findById(id);
    }

    public void touchLastSeen(Long id) {
        if (id == null) return;
        watchlistRepository.findById(id).ifPresent(w -> {
            w.setLastSeenAt(LocalDateTime.now());
            watchlistRepository.save(w);
        });
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

    public List<Watchlist> getAllWatchlists() {
        return watchlistRepository.findAll();
    }

    @Transactional
    public boolean deleteWatchlist(Long watchlistId) {
        if (watchlistId == null || watchlistId == 1L) {
            return false; // Protect primary default watchlist
        }
        if (watchlistRepository.existsById(watchlistId)) {
            watchlistStockRepository.deleteByWatchlistId(watchlistId);
            watchlistRepository.deleteById(watchlistId);
            return true;
        }
        return false;
    }

    @Transactional
    public Watchlist loadBasketIntoWatchlist(Long watchlistId, List<String> symbols) {
        if (watchlistId == null || symbols == null) return null;
        watchlistStockRepository.deleteByWatchlistId(watchlistId);
        for (String sym : symbols) {
            if (sym != null && !sym.isBlank()) {
                WatchlistStock stock = new WatchlistStock(watchlistId, sym.trim().toUpperCase(), LocalDateTime.now());
                watchlistStockRepository.save(stock);
            }
        }
        return watchlistRepository.findById(watchlistId).orElse(null);
    }
}
