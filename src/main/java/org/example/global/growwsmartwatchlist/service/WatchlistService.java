package org.example.global.growwsmartwatchlist.service;

import org.example.global.growwsmartwatchlist.model.WatchlistStock;
import org.example.global.growwsmartwatchlist.repository.WatchlistStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WatchlistService {

    @Autowired
    private WatchlistStockRepository watchlistStockRepository;

    public boolean addStock(Long watchlistId, String symbol) {
        if (watchlistId == null || symbol == null || symbol.trim().isEmpty()) {
            return false;
        }
        WatchlistStock stock = new WatchlistStock(watchlistId, symbol.toUpperCase().trim(), LocalDateTime.now());
        watchlistStockRepository.save(stock);
        return true;
    }
}
