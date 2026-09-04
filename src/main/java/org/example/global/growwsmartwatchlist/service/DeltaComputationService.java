package org.example.global.growwsmartwatchlist.service;

import org.example.global.growwsmartwatchlist.feed.MarketDataFeed;
import org.example.global.growwsmartwatchlist.model.ActiveMover;
import org.example.global.growwsmartwatchlist.model.DeltaResponse;
import org.example.global.growwsmartwatchlist.model.StockTick;
import org.example.global.growwsmartwatchlist.model.WatchlistStock;
import org.example.global.growwsmartwatchlist.repository.WatchlistStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeltaComputationService {

    private static final double ACTIVE_THRESHOLD = 0.85;

    @Autowired
    private WatchlistStockRepository watchlistStockRepository;

    @Autowired
    private MarketDataFeed marketDataFeed;

    @Autowired
    private ScoringEngineService scoringEngineService;

    public DeltaResponse computeRankedDelta(Long watchlistId, String anchorMode) {
        List<WatchlistStock> watchlistStocks = watchlistStockRepository.findAll().stream()
                .filter(ws -> ws.getWatchlistId().equals(watchlistId))
                .toList();

        List<ActiveMover> activeMovers = new ArrayList<>();
        int quietCount = 0;

        for (WatchlistStock ws : watchlistStocks) {
            String symbol = ws.getSymbol();
            List<StockTick> ticks = marketDataFeed.getTicksForSymbol(symbol);
            double currentPrice = 0.0;
            long currentVolume = 0;
            List<Double> histPrices = new ArrayList<>();
            List<Long> histVolumes = new ArrayList<>();

            for (StockTick tick : ticks) {
                histPrices.add(tick.getPrice());
                histVolumes.add(tick.getVolume());
                currentPrice = tick.getPrice();
                currentVolume = tick.getVolume();
            }

            double score = scoringEngineService.calculateCompositeScore(
                    histPrices, currentPrice, histVolumes, currentVolume, currentPrice * 1.15, currentPrice * 0.85, false
            );

            ActiveMover mover = new ActiveMover(
                    symbol, symbol, currentPrice, 0.0, score, "Volume Surge", false, ""
            );

            if (score >= ACTIVE_THRESHOLD) {
                activeMovers.add(mover);
            } else {
                quietCount++;
            }
        }

        activeMovers.sort((a, b) -> Double.compare(b.getCompositeScore(), a.getCompositeScore()));

        return new DeltaResponse(watchlistId, anchorMode != null ? anchorMode : "SINCE_LAST_SEEN", activeMovers, quietCount);
    }
}
