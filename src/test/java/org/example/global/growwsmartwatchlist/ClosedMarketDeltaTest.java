package org.example.global.growwsmartwatchlist;

import org.example.global.growwsmartwatchlist.model.ActiveMover;
import org.example.global.growwsmartwatchlist.model.DeltaResponse;
import org.example.global.growwsmartwatchlist.model.Watchlist;
import org.example.global.growwsmartwatchlist.service.DeltaComputationService;
import org.example.global.growwsmartwatchlist.service.WatchlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ClosedMarketDeltaTest {

    @Autowired
    private WatchlistService watchlistService;

    @Autowired
    private DeltaComputationService deltaComputationService;

    @Test
    void testClosedMarketDeltaReturnsIdenticalPricesAcrossCalls() throws InterruptedException {
        // 1. Create an isolated test watchlist and add stocks for this test
        Watchlist testWatchlist = watchlistService.createWatchlist(888L, "Closed Market Test Watchlist");
        Long watchlistId = testWatchlist.getId();
        watchlistService.addStock(watchlistId, "RELIANCE");
        watchlistService.addStock(watchlistId, "TCS");

        // 2. Inject a fixed clock representing a closed market session (Sunday 20:00 IST)
        Instant closedTimestamp = Instant.parse("2026-09-06T14:30:00Z"); // Sunday
        deltaComputationService.setClock(Clock.fixed(closedTimestamp, ZoneId.of("Asia/Kolkata")));

        // 3. Make two consecutive delta calls
        DeltaResponse response1 = deltaComputationService.computeRankedDelta(watchlistId, "SINCE_OPEN");
        Thread.sleep(50);
        DeltaResponse response2 = deltaComputationService.computeRankedDelta(watchlistId, "SINCE_OPEN");

        // 4. Assert market is closed and status text reads correctly
        assertFalse(response1.isMarketOpen(), "Market should be reported as closed");
        assertFalse(response2.isMarketOpen(), "Market should be reported as closed");
        assertEquals("Closed — Last close 15:30 IST", response1.getMarketStatusText());
        assertEquals("Closed — Last close 15:30 IST", response2.getMarketStatusText());

        // 5. Assert active movers and quiet counts match 1:1
        assertEquals(response1.getActiveMovers().size(), response2.getActiveMovers().size());
        assertEquals(response1.getQuietCount(), response2.getQuietCount());

        // 6. Assert exact price, anchorPrice, and deltaPercent equality across calls
        for (int i = 0; i < response1.getActiveMovers().size(); i++) {
            ActiveMover m1 = response1.getActiveMovers().get(i);
            ActiveMover m2 = response2.getActiveMovers().get(i);

            assertEquals(m1.getSymbol(), m2.getSymbol());
            assertEquals(m1.getCurrentPrice(), m2.getCurrentPrice(), 0.0001,
                    "Current price for " + m1.getSymbol() + " must be frozen and identical");
            assertEquals(m1.getAnchorPrice(), m2.getAnchorPrice(), 0.0001,
                    "Anchor price for " + m1.getSymbol() + " must be frozen and identical");
            assertEquals(m1.getDeltaPercent(), m2.getDeltaPercent(), 0.0001,
                    "Delta percent for " + m1.getSymbol() + " must be frozen and identical");
        }
    }
}
