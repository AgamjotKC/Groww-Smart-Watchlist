package org.example.global.growwsmartwatchlist.repository;

import org.example.global.growwsmartwatchlist.model.WatchlistStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistStockRepository extends JpaRepository<WatchlistStock, Long> {
    boolean existsByWatchlistIdAndSymbol(Long watchlistId, String symbol);
}
