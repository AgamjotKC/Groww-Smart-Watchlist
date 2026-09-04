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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DeltaComputationService {

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
        String anchorTimeString = lastSeen.format(DateTimeFormatter.ofPattern("HH:mm"));

        List<ActiveMover> activeMovers = new ArrayList<>();
        List<ActiveMover> quietStocks = new ArrayList<>();
        boolean isAnyDelayed = false;

        double sumDelta = 0.0;
        int totalTracked = 0;

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
                refPrice = "SINCE_OPEN".equals(normalizedAnchor) ? q.openPrice : q.prevClose;
                deltaPercent = refPrice > 0 ? ((currentPrice - refPrice) / refPrice) * 100.0 : q.deltaPercent;
                volume = q.volume;
                week52High = q.week52High;
                week52Low = q.week52Low;
                if (q.companyName != null && !q.companyName.isBlank()) {
                    companyName = q.companyName;
                }
                if (q.isCachedFallback) {
                    isAnyDelayed = true;
                }

                histPrices = q.candlePrices.isEmpty()
                        ? List.of(refPrice, (refPrice + currentPrice) / 2.0, currentPrice)
                        : q.candlePrices;

                histVolumes = q.candleVolumes.isEmpty()
                        ? List.of((long)(volume * 0.8), (long)(volume * 0.9), volume)
                        : q.candleVolumes;

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

            sumDelta += deltaPercent;
            totalTracked++;

            boolean hasCatalyst = catalystService.hasCatalyst(symbol);
            Optional<Catalyst> catalystOpt = catalystService.getLatestCatalyst(symbol);
            String catalystText = catalystOpt.map(c -> c.getEventType() + ": " + c.getTitle()).orElse("");

            double volatilityZScore = scoringEngineService.calculateVolatilityZScore(histPrices, currentPrice);
            double volumeSurgeRatio = scoringEngineService.calculateVolumeSurgeRatio(histVolumes, volume);

            double score = scoringEngineService.calculateCompositeScore(
                    histPrices, currentPrice, histVolumes, volume, week52High, week52Low, hasCatalyst
            );

            String newsUrl = "https://www.google.com/finance/quote/" + symbol.toUpperCase() + ":NSE";

            ActiveMover mover = new ActiveMover(
                    symbol, companyName, currentPrice, refPrice, anchorTimeString, deltaPercent, score, "Volume Surge",
                    hasCatalyst, catalystText, newsUrl, volumeSurgeRatio, volatilityZScore,
                    week52High, week52Low
            );

            // Calibrated Active vs Quiet Partitioning Logic
            boolean near52w = (week52High > 0 && currentPrice >= 0.99 * week52High);
            boolean isActiveMover = (Math.abs(deltaPercent) >= 0.8 && volumeSurgeRatio >= 1.4)
                                 || (volatilityZScore >= 1.5)
                                 || near52w
                                 || hasCatalyst;

            if (isActiveMover) {
                activeMovers.add(mover);
            } else {
                quietStocks.add(mover); // Flat stocks (+0.00%, low vol) MUST land here cleanly
            }
        }

        activeMovers.sort((a, b) -> Double.compare(b.getCompositeScore(), a.getCompositeScore()));
        quietStocks.sort((a, b) -> Double.compare(Math.abs(b.getDeltaPercent()), Math.abs(a.getDeltaPercent())));

        // Generate Dynamic Executive Synthesis Summary String
        double avgDelta = totalTracked > 0 ? sumDelta / totalTracked : 0.0;
        String toneStr = avgDelta >= 0.5 ? "bullish" : (avgDelta <= -0.5 ? "bearish" : "neutral");
        String formattedAvg = (avgDelta >= 0 ? "+" : "") + String.format("%.2f", avgDelta) + "%";

        String synthesisText;
        if (!activeMovers.isEmpty()) {
            ActiveMover topMover = activeMovers.get(0);
            String moverDetail = topMover.getSymbol() + " (" + (topMover.getDeltaPercent() >= 0 ? "+" : "") + String.format("%.1f", topMover.getDeltaPercent()) + "%)";
            synthesisText = String.format(
                    "Since your last session (%s): %d %s broke momentum with abnormal volume led by %s, while %d %s remained quiet with minimal price churn. Market tone across your basket is %s (%s).",
                    anchorTimeString,
                    activeMovers.size(), activeMovers.size() == 1 ? "stock" : "stocks",
                    moverDetail,
                    quietStocks.size(), quietStocks.size() == 1 ? "stock" : "stocks",
                    toneStr, formattedAvg
            );
        } else {
            synthesisText = String.format(
                    "Since your last session (%s): All %d stocks in your basket are trading within normal calm bounds with minimal price churn. Market tone is %s (%s).",
                    anchorTimeString, totalTracked, toneStr, formattedAvg
            );
        }

        DeltaResponse response = new DeltaResponse(watchlistId, normalizedAnchor, activeMovers, quietStocks.size(), quietStocks);
        response.setLastSeenAt(lastSeenIso);
        response.setSynthesisSummary(synthesisText);
        response.setDelayedFallback(isAnyDelayed);

        return response;
    }
}
