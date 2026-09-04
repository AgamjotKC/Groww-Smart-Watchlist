package org.example.global.growwsmartwatchlist;

import org.example.global.growwsmartwatchlist.service.WatchlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
}
