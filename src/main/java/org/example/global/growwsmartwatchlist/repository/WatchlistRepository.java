package org.example.global.growwsmartwatchlist.repository;

import org.example.global.growwsmartwatchlist.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
}
