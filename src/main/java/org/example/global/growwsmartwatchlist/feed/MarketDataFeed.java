package org.example.global.growwsmartwatchlist.feed;

import org.example.global.growwsmartwatchlist.model.StockTick;
import java.util.List;

public interface MarketDataFeed {
    List<StockTick> getLatestTicks();
    List<StockTick> getTicksForSymbol(String symbol);
    StockTick getLatestTick(String symbol);
}
