package org.example.global.growwsmartwatchlist.service;

import org.example.global.growwsmartwatchlist.model.WatchlistStock;
import org.example.global.growwsmartwatchlist.repository.WatchlistStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class WatchlistService {

    @Autowired
    private WatchlistStockRepository watchlistStockRepository;

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
