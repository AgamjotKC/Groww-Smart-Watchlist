# Groww Catch-Up — Smart Market Watchlist & Session Diff Engine 📈

> **Submission for "Code, by Groww" Hackathon 2026**  
> *Author: Agamjot Kaur*  
> *Theme: What to build? - CODE 2026 (Build a Smart Market Watchlist)*

---

## 📄 Exact 100-Word Product Pitch

**Groww Catch-Up** turns noisy watchlists into session-aware market intelligence. Instead of scanning raw prices, users instantly see what meaningfully changed since their last visit or market open. 

Built with Java 17, Spring Boot, and Vanilla JS, Catch-Up partitions watchlists into **Active Movers**—ranked by statistical volatility Z-scores ($Z \ge 2.0$), volume surges ($\ge 2.0\times$), 52-week extremes, official NSE corporate filings, and 3-day SMA trendline crossovers—and collapses normal churn into **Quiet Stocks**. 

It handles stale or off-market data via dual-feed fallbacks (live NSE $\rightarrow$ Yahoo Finance) and 15:30 EOD price-clamping, ensuring zero false diffs while markets are closed.

---

## 🌟 Key Engineering Highlights

### 1. Dynamic Session Anchoring & State Persistence
- **Since Last Seen**: Computes price/volume deltas relative to the user's server-persisted `Watchlist.lastSeenAt` timestamp.
- **Since Open**: Computes deltas relative to the 09:15 AM IST market open baseline.
- State is persisted across sessions in H2 JPA database tables (`Watchlist`, `WatchlistStock`, `StockTick`, `Catalyst`).

### 2. Multi-Factor Statistical Relevance Engine (`ScoringEngineService`)
Stocks are ranked dynamically by a composite relevance score combining:
$$\text{Composite Score} = f(\text{Price Delta \%}, \text{Volatility Z-Score}, \text{Volume Surge Ratio}, \text{52W Extremes}, \text{Material Filings})$$
- **Volatility Z-Score**: $Z = \frac{P_{current} - \mu_{history}}{\sigma_{history}}$ measures standard deviation deviation from rolling baseline.
- **Volume Surge Ratio**: $\frac{\text{Current Volume}}{\text{20-Day Average Daily Volume (ADV)}}$.

### 3. Isolated Technical Crossover Engine (`TrendlineService`)
- Fetches 60-day daily closing price histories (`GET /v8/finance/chart/{symbol}.NS?range=3mo&interval=1d`).
- Computes $SMA_{20}$ and $SMA_{50}$.
- Flags **✨ Golden Cross** ($SMA_{20} > SMA_{50}$) or **💀 Death Cross** ($SMA_{20} < SMA_{50}$) **only** when a state transition occurred within the last 3 trading days.

### 4. Handling Stale, Delayed, and Conflicting Data
- **Primary & Fallback Feed Pipeline**: Primary live cookie-warmed NSE provider $\rightarrow$ Yahoo Finance chart API $\rightarrow$ Mock tick provider.
- **Visual Freshness Badge**: Surfaces data status explicitly in the header (`🟢 Live NSE Feed` vs `Yahoo EOD/Delayed`).
- **Deterministic 15:30 EOD Price-Clamping**: When market is closed (after 15:30 IST or weekends), prices freeze at the official 15:30 close, preventing false diff fluctuations while market is closed.

### 5. Full Watchlist Management & Sectoral Baskets
- Create new empty watchlists with custom names (`POST /api/watchlists`).
- Add stocks via live NSE autocomplete search (`POST /api/watchlists/{id}/stocks`).
- Remove stocks (`DELETE /api/watchlists/{id}/stocks/{symbol}`).
- Delete custom watchlists (`DELETE /api/watchlists/{id}`).
- Load official NSE sectoral baskets: Nifty IT, Nifty Bank, Nifty Auto, Nifty Pharma, Nifty FMCG, Nifty Metal, Nifty PSU Bank, Nifty Realty.

---

## ⚡ How to Run

### Option 1: Docker & Docker Compose (Recommended)
```bash
docker compose up -d
```
Open **[http://localhost:8080](http://localhost:8080)** in your browser.

### Option 2: Local Maven Execution
```bash
# Windows PowerShell
.\mvnw spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```
Open **[http://localhost:8080](http://localhost:8080)** in your browser.

### Option 3: Run Automated Tests
```bash
.\mvnw test
```
*(Runs all 21 unit and integration tests cleanly).*

---

## 📡 REST API Reference

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/watchlists` | `GET` | Get all user watchlists |
| `/api/watchlists` | `POST` | Create a new custom watchlist |
| `/api/watchlists/{id}` | `DELETE` | Delete custom watchlist |
| `/api/watchlists/{id}/delta?anchor=SINCE_LAST_SEEN` | `GET` | Get partitioned session market diffs |
| `/api/watchlists/{id}/stocks` | `POST` | Add stock symbol to watchlist |
| `/api/watchlists/{id}/stocks/{symbol}` | `DELETE` | Remove stock symbol from watchlist |
| `/api/watchlists/{id}/basket` | `POST` | Load a curated basket into watchlist |
| `/api/baskets` | `GET` | List all curated NSE sectoral baskets |
| `/api/stocks/search?q={query}` | `GET` | Autocomplete NSE stock search |

---

## 📐 Scalability & Architectural Trade-offs

1. **Restraint Over Clutter**: Kept scoring rules deterministic and statistical ($Z$-scores) rather than adding opaque ML black boxes.
2. **In-Memory Thread-Safe Caching**: Used `ConcurrentHashMap` caches in `TrendlineService` and `NseMarketDataProvider` to minimize external HTTP overhead and protect third-party rate limits.
3. **Graceful Fallbacks**: Primary live feeds degrade smoothly to delayed feeds with visible UI indicators, ensuring 100% uptime for end users.
