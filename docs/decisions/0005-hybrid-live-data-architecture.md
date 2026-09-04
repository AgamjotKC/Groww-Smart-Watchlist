# ADR 0005: Hybrid Live Data Architecture (Yahoo Finance Quotes + NSE Corporate Announcements)

## Context
During direct live market data integration testing against National Stock Exchange of India (NSE) endpoints, empirical network inspection revealed a key architectural divergence:
- **Equity Quote API Blockage**: Direct outbound HTTP requests to NSE equity quote endpoints (`https://www.nseindia.com/api/quote-equity?symbol=...`) return `HTTP 403 Forbidden` due to Akamai bot mitigation / WAF enforcement on non-browser automated HTTP client sessions.
- **Corporate Announcement API Uptime**: Direct outbound GET calls to NSE corporate announcement endpoints (`https://www.nseindia.com/api/corporate-announcements?index=equities&symbol=...`) consistently succeed with `HTTP 200 OK`, serving authentic live corporate filings along with direct attachment document links on `nsearchives.nseindia.com`.

## Decision
To finalize a robust data layer for Indian equity markets without sacrificing live regulatory filing access or hitting WAF walls, we adopt a **Hybrid Live Data Architecture**:

1. **Market Price & Volume Feed**: Route market quote and intraday price chart ingestion through Yahoo Finance chart API (`https://query1.finance.yahoo.com/v8/finance/chart/{SYMBOL}.NS?range=1d&interval=5m`), with parsing verification over `indicators.quote[0].close` and static baseline fallback levels.
2. **Corporate Disclosure & Catalyst Feed**: Ingest primary corporate disclosure metadata directly from official NSE APIs (`nseindia.com/api/corporate-announcements`) utilizing automated session cookie warm-up (`https://www.nseindia.com`).
3. **Broadened Taxonomy Normalization**: Replace rigid string checks with case-insensitive keyword inspection across raw disclosure descriptions (`desc`) and attachment text (`attchmntText`) in `CatalystService.java` to map filings into standardized categories:
   - `Dividend` (💰)
   - `Earnings` (📈)
   - `Board Meeting` (🏛️)
   - `M&A / Partnership` (🤝)
   - `Contract Win` (📜)
   - `Management` (👔)
   - `Corporate Update` (📑 clean fallback)

## Consequences
- Guarantees high-availability market data access for NSE equities while maintaining zero-delay access to official regulatory disclosures.
- Transparently communicates feed sources in the user interface (`Yahoo Finance (EOD/Delayed)` for price quotes and `🟢 Verified Live NSE Filings` for disclosure catalysts).
- Eliminates brittle reliance on WAF-blocked direct equity quote endpoints while maintaining extensible interfaces (`MarketDataFeed` and `NseMarketDataProvider`).
