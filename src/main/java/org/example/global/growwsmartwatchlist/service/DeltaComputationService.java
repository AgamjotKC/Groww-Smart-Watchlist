package org.example.global.growwsmartwatchlist.service;

import org.example.global.growwsmartwatchlist.feed.MarketDataFeed;
import org.example.global.growwsmartwatchlist.feed.NseMarketDataProvider;
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
    private NseMarketDataProvider nseMarketDataProvider;

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
        String lastSeenIso = lastSeen.toString();

        List<ActiveMover> activeMovers = new ArrayList<>();
        List<ActiveMover> quietStocks = new ArrayList<>();

        for (WatchlistStock ws : watchlistStocks) {
            String symbol = ws.getSymbol();
            Optional<NseMarketDataProvider.NseQuoteCache> nseQuoteOpt = nseMarketDataProvider.fetchNseQuote(symbol);

            double currentPrice;
            double refPrice;
            double deltaPercent;
            long volume;
            double week52High;
            double week52Low;
            String companyName = symbol;

            List<Double> histPrices = new ArrayList<>();
            List<Long> histVolumes = new ArrayList<>();

            if (nseQuoteOpt.isPresent()) {
                NseMarketDataProvider.NseQuoteCache q = nseQuoteOpt.get();
                currentPrice = q.currentPrice;
                refPrice = q.prevClose;
                deltaPercent = q.deltaPercent;
                volume = q.volume;
                week52High = q.week52High;
                week52Low = q.week52Low;
                if (q.companyName != null && !q.companyName.isBlank()) {
                    companyName = q.companyName;
                }

                // Simulate price trajectory for scoring window
                histPrices = List.of(refPrice, (refPrice + currentPrice) / 2.0, currentPrice);
                histVolumes = List.of((long)(volume * 0.8), (long)(volume * 0.9), volume);
            } else {
                List<StockTick> ticks = marketDataFeed.getTicksForSymbol(symbol);
                if (ticks.isEmpty()) continue;

                currentPrice = ticks.get(ticks.size() - 1).getPrice();
                volume = ticks.get(ticks.size() - 1).getVolume();

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
                deltaPercent = refPrice > 0 ? ((currentPrice - refPrice) / refPrice) * 100.0 : 0.0;
                histPrices = ticks.stream().map(StockTick::getPrice).toList();
                histVolumes = ticks.stream().map(StockTick::getVolume).toList();
                week52High = currentPrice * 1.15;
                week52Low = currentPrice * 0.85;
            }

            boolean hasCatalyst = catalystService.hasCatalyst(symbol);
            Optional<Catalyst> catalystOpt = catalystService.getLatestCatalyst(symbol);
            String catalystText = catalystOpt.map(c -> c.getEventType() + ": " + c.getTitle()).orElse("");

            double score = scoringEngineService.calculateCompositeScore(
                    histPrices, currentPrice, histVolumes, volume, week52High, week52Low, hasCatalyst
            );

            ActiveMover mover = new ActiveMover(
                    symbol, companyName, currentPrice, deltaPercent, score, "Volume Surge", hasCatalyst, catalystText
            );

            if (score >= ACTIVE_THRESHOLD || Math.abs(deltaPercent) >= 0.5) {
                activeMovers.add(mover);
            } else {
                quietStocks.add(mover);
            }
        }

        activeMovers.sort((a, b) -> Double.compare(b.getCompositeScore(), a.getCompositeScore()));
        quietStocks.sort((a, b) -> Double.compare(Math.abs(b.getDeltaPercent()), Math.abs(a.getDeltaPercent())));

        DeltaResponse response = new DeltaResponse(watchlistId, normalizedAnchor, activeMovers, quietStocks.size(), quietStocks);
        response.setLastSeenAt(lastSeenIso);
        return response;
    }
}
