package org.example.global.growwsmartwatchlist;

import org.example.global.growwsmartwatchlist.service.WatchlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
