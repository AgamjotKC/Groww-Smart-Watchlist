# Catch-Up 📈

> **Groww Solo Hackathon — Code 2026 Submission**  
> *A Restrained, High-Signal Market Diff Engine & Smart Watchlist Surface.*

---

## 🌟 Product Framing
Retail stock market watchlists are often cluttered with dozens of static price feeds, causing information overload and cognitive fatigue. **Catch-Up** reimagines the watchlist experience around **restraint and high-signal diffing**. Instead of scanning raw stock prices, Catch-Up provides an instant summary of *what moved and why* since the user last checked the market.

---

## 🚀 The 4 Core Capabilities

| Capability | How It Was Built |
| :--- | :--- |
| **1. Multi-Anchor Delta Computation** | Dual calculation paths (`since_last_seen` and `since_open`). `since_last_seen` tracks individual user reference timestamps stored server-side per watchlist session, while `since_open` compares against session baselines. |
| **2. Composite Relevance Engine** | Ranks stocks using statistical Z-Score normalization: `Composite = Z(Price Change) + Z(Volume Surge) + 52w Level Proximity + Catalyst Boost`. New listings automatically default to neutral metrics ($Z=0$). |
| **3. Active vs. Quiet Partitioning** | Dynamically splits watchlists into **Active Movers** ($\ge 1.0$ composite score threshold) and collapses low-activity noise into a sleek **Quiet Stocks Drawer**. |
| **4. Event Catalysts & Live Feed Replay** | Integrated tick streaming feed replay (`mock_ticks.json`) and corporate filings feed (`mock_filings.json`) surfacing Earnings, Corporate Actions, and News badges. |

---

## 🛠 Tech Stack & Architecture
- **Backend Framework**: Java 17, Spring Boot 3.4.3, Spring Data JPA
- **Database**: H2 In-Memory DB (`jdbc:h2:mem:catchupdb`)
- **Frontend**: Vanilla HTML5, Modern CSS3 (Groww Light Theme, glassmorphism, responsive grid), Vanilla JavaScript (ES6 Modules/Fetch API)
- **Testing**: JUnit 5, Spring Boot Test (`./mvnw test`)

---

## ⚡ Setup & Quickstart

### Prerequisites
- JDK 17 or higher
- Maven 3.8+ (wrapper included)

### Running the Application Locally
```bash
# 1. Clone the repository
git clone https://github.com/AgamjotKC/Groww-Smart-Watchlist.git
cd Groww-Smart-Watchlist

# 2. Run Spring Boot application
./mvnw spring-boot:run
# On Windows PowerShell:
# .\mvnw spring-boot:run
```

Once started, open your web browser and visit:
👉 **[http://localhost:8080](http://localhost:8080)**

---

## 📡 REST API Summary

- **GET `/api/watchlists/{id}/delta?anchor=SINCE_LAST_SEEN`** — Returns partitioned Catch-Up delta response.
- **POST `/api/watchlists`** — Create a new custom watchlist.
- **POST `/api/watchlists/{id}/stocks`** — Add a stock symbol to a watchlist.
- **GET `/api/baskets`** — List prebuilt curated baskets (Nifty Tech Leaders, Banking Giants, Green Energy).

---

## 🔮 What I'd Build Next
1. **WebSocket Push Protocol**: Transition from poll-based REST to Server-Sent Events (SSE) or WebSockets for real-time score updates.
2. **Personalized Adaptive Thresholds**: Dynamically adjust the Active/Quiet partition threshold based on individual user visit patterns and volatility tolerances.
3. **Mobile Push Digest**: Send automated morning/evening Catch-Up push notifications summarizing key portfolio diffs.
