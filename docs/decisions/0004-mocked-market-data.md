# ADR 0004: Mocked Replayable Market Ticks & Corporate Filings Feed

## Context
Market trading hackathon submissions require reliable, 100% deterministic evaluation regardless of external exchange API status, rate limits, or off-market hours.

## Decision
Implement market tick ingestion and corporate event catalysts against abstract interfaces (`MarketDataFeed` and `CatalystService`) backed by JSON files (`mock_ticks.json` and `mock_filings.json`).

## Consequences
- Guarantees instant, repeatable demonstration of volatility scoring, volume surge detection, and catalyst badges without relying on live WebSocket uptime.
- Production migration requires replacing feed implementations with exchange WebSocket/XBRL webhooks without modifying core scoring or delta computation logic.
