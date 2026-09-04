package org.example.global.growwsmartwatchlist.repository;

import org.example.global.growwsmartwatchlist.model.WatchlistStock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WatchlistStockRepository extends JpaRepository<WatchlistStock, Long> {
    boolean existsByWatchlistIdAndSymbol(Long watchlistId, String symbol);
    Optional<WatchlistStock> findByWatchlistIdAndSymbol(Long watchlistId, String symbol);
    void deleteByWatchlistIdAndSymbol(Long watchlistId, String symbol);
}
