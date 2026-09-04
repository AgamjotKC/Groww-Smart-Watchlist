package org.example.global.growwsmartwatchlist.service;

import org.example.global.growwsmartwatchlist.feed.MarketDataFeed;
import org.example.global.growwsmartwatchlist.model.ActiveMover;
import org.example.global.growwsmartwatchlist.model.Catalyst;
import org.example.global.growwsmartwatchlist.model.DeltaResponse;
import org.example.global.growwsmartwatchlist.model.StockTick;
import org.example.global.growwsmartwatchlist.model.Watchlist;
import org.example.global.growwsmartwatchlist.model.WatchlistStock;
import org.example.global.growwsmartwatchlist.repository.WatchlistRepository;
import org.example.global.growwsmartwatchlist.repository.WatchlistStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DeltaComputationService {

    private static final double ACTIVE_THRESHOLD = 1.0;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private WatchlistStockRepository watchlistStockRepository;

    @Autowired
    private MarketDataFeed marketDataFeed;

    @Autowired
    private ScoringEngineService scoringEngineService;

    @Autowired
    private CatalystService catalystService;

    public DeltaResponse computeRankedDelta(Long watchlistId, String anchorMode) {
        String normalizedAnchor = (anchorMode != null && anchorMode.equalsIgnoreCase("SINCE_OPEN")) ? "SINCE_OPEN" : "SINCE_LAST_SEEN";

        List<WatchlistStock> watchlistStocks = watchlistStockRepository.findAll().stream()
                .filter(ws -> ws.getWatchlistId().equals(watchlistId))
                .toList();

        Optional<Watchlist> watchlistOpt = watchlistRepository.findById(watchlistId);
        LocalDateTime lastSeen = watchlistOpt.map(Watchlist::getLastSeenAt).orElse(LocalDateTime.now().minusHours(24));

        List<ActiveMover> activeMovers = new ArrayList<>();
        int quietCount = 0;

        for (WatchlistStock ws : watchlistStocks) {
            String symbol = ws.getSymbol();
            List<StockTick> ticks = marketDataFeed.getTicksForSymbol(symbol);
            if (ticks.isEmpty()) continue;

            double currentPrice = ticks.get(ticks.size() - 1).getPrice();
            long currentVolume = ticks.get(ticks.size() - 1).getVolume();

            double refPrice;
            if ("SINCE_OPEN".equals(normalizedAnchor)) {
                refPrice = ticks.get(0).getPrice();
            } else {
                refPrice = ticks.get(0).getPrice();
                for (StockTick t : ticks) {
                    if (t.getTimestamp() != null && t.getTimestamp().isBefore(lastSeen.toInstant(ZoneOffset.UTC))) {
                        refPrice = t.getPrice();
                    }
                }
            }

            double deltaPercent = refPrice > 0 ? ((currentPrice - refPrice) / refPrice) * 100.0 : 0.0;

            List<Double> histPrices = ticks.stream().map(StockTick::getPrice).toList();
            List<Long> histVolumes = ticks.stream().map(StockTick::getVolume).toList();

            boolean hasCatalyst = catalystService.hasCatalyst(symbol);
            Optional<Catalyst> catalystOpt = catalystService.getLatestCatalyst(symbol);
            String catalystText = catalystOpt.map(c -> c.getEventType() + ": " + c.getTitle()).orElse("");

            double score = scoringEngineService.calculateCompositeScore(
                    histPrices, currentPrice, histVolumes, currentVolume, currentPrice * 1.15, currentPrice * 0.85, hasCatalyst
            );

            ActiveMover mover = new ActiveMover(
                    symbol, symbol, currentPrice, deltaPercent, score, "Volume Surge", hasCatalyst, catalystText
            );

            if (score >= ACTIVE_THRESHOLD) {
                activeMovers.add(mover);
            } else {
                quietCount++;
            }
        }

        activeMovers.sort((a, b) -> Double.compare(b.getCompositeScore(), a.getCompositeScore()));

        return new DeltaResponse(watchlistId, normalizedAnchor, activeMovers, quietCount);
    }
}
