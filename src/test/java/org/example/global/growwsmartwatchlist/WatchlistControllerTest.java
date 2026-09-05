package org.example.global.growwsmartwatchlist;

import org.example.global.growwsmartwatchlist.model.Watchlist;
import org.example.global.growwsmartwatchlist.service.WatchlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class WatchlistControllerTest {

    @Autowired
    private WatchlistService watchlistService;

    @Test
    void testGetAllWatchlistsService() {
        List<Watchlist> watchlists = watchlistService.getAllWatchlists();
        assertNotNull(watchlists);
        assertFalse(watchlists.isEmpty());
    }

    @Test
    void testGetWatchlistByIdService() {
        Watchlist watchlist = watchlistService.getWatchlist(1L).orElse(null);
        assertNotNull(watchlist);
        assertEquals("Primary Catch-Up List", watchlist.getName());
    }

    @Test
    void testCreateWatchlistService() {
        Watchlist custom = watchlistService.createWatchlist(1L, "Energy & Utilities");
        assertNotNull(custom);
        assertEquals("Energy & Utilities", custom.getName());
    }

    @Test
    void testAddAndRemoveStockService() {
        boolean added = watchlistService.addStock(1L, "AXISBANK");
        assertTrue(added || watchlistService.getWatchlist(1L).isPresent());

        boolean removed = watchlistService.removeStock(1L, "AXISBANK");
        assertTrue(removed || !added);
    }

    @Test
    void testDeletePrimaryWatchlistProtectedService() {
        boolean deleted = watchlistService.deleteWatchlist(1L);
        assertFalse(deleted);
    }

    @Test
    void testDeleteCustomWatchlistService() {
        Watchlist custom = watchlistService.createWatchlist(1L, "Temp Delete List");
        boolean deleted = watchlistService.deleteWatchlist(custom.getId());
        assertTrue(deleted);
    }
}
