package org.example.global.growwsmartwatchlist;

import org.example.global.growwsmartwatchlist.model.Watchlist;
import org.example.global.growwsmartwatchlist.service.WatchlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class WatchlistServiceTest {

    @Autowired
    private WatchlistService watchlistService;

    @Test
    void testAddStockToWatchlist() {
        boolean result = watchlistService.addStock(1L, "RELIANCE");
        assertTrue(result);
    }

    @Test
    void testAddDuplicateStockToWatchlist() {
        watchlistService.addStock(2L, "TCS");
        boolean duplicateResult = watchlistService.addStock(2L, "TCS");
        assertFalse(duplicateResult);
    }

    @Test
    void testRemoveStockFromWatchlist() {
        watchlistService.addStock(3L, "INFY");
        boolean removed = watchlistService.removeStock(3L, "INFY");
        assertTrue(removed);

        boolean removeAgain = watchlistService.removeStock(3L, "INFY");
        assertFalse(removeAgain);
    }

    @Test
    void testGetWatchlistUpdatesLastSeenAt() throws InterruptedException {
        Watchlist created = watchlistService.createWatchlist(100L, "Tech Delta");
        LocalDateTime initialLastSeen = created.getLastSeenAt();

        Thread.sleep(20);

        Watchlist fetched = watchlistService.getWatchlist(created.getId()).orElseThrow();
        assertNotNull(fetched.getLastSeenAt());
    }
}
