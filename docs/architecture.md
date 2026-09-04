# System & Solution Architecture — Catch-Up

## Overview
Catch-Up is structured around a signal-processing pipeline:
1. **Market Data Ingestion**: Consumes ticks via `MarketDataFeed` interface. Mocked against sample JSON streams for deterministic hackathon evaluation. Production implementation would consume exchange WebSockets.
2. **Pure Scoring Engine**: Calculates Z-score of price movement relative to rolling mean and stddev, volume surge ratio, level proximity, and catalyst boosts.
3. **Delta Computation Endpoint**: Computes active vs. quiet partitioning for a given watchlist against `last_seen_at`.
4. **Catch-Up Card Surface**: UI component presenting high-signal active movers and a collapsed quiet drawer.

## Data Layer Split
- **Durable Relational Model**: Users, Watchlists, Watchlist Stocks, Catalysts, Baskets.
- **In-Memory Cache**: Live rolling tick windows, precomputed symbol relevance scores, delta snapshots.
