let currentAnchor = "SINCE_LAST_SEEN";
let currentWatchlistId = 1;
let pollTimer = null;

let isDemoMode = false;
let currentSortBy = "relevance";
let rawCatchUpData = null;

let currentOrderSymbol = "";
let currentOrderCompany = "";
let currentOrderPrice = 0;
let currentOrderDelta = 0;
let currentOrderType = "Delivery";

function isMarketOpenNow() {
    try {
        const now = new Date();
        const kolkataTimeString = now.toLocaleString("en-US", { timeZone: "Asia/Kolkata" });
        const kolkataDate = new Date(kolkataTimeString);

        const day = kolkataDate.getDay();
        if (day === 0 || day === 6) return false;

        const hours = kolkataDate.getHours();
        const minutes = kolkataDate.getMinutes();
        const timeInMins = hours * 60 + minutes;

        return timeInMins >= 555 && timeInMins < 930;
    } catch (e) {
        return false;
    }
}

function setFreshness(status, isDelayed, apiIsMarketOpen) {
    const badge = document.getElementById("freshness-badge");
    const dot = document.getElementById("freshness-dot");
    const lastSeenText = document.getElementById("last-seen-text");
    if (!badge || !dot) return;

    if (isDemoMode) {
        badge.className = "freshness-badge demo";
        badge.innerText = "Demo Mode — Simulated Data";
        dot.className = "dot demo";
        if (lastSeenText) {
            lastSeenText.innerText = "Demo Mode active: Surfacing deterministic simulated diffs for live demonstration.";
        }
        return;
    }

    const marketOpen = apiIsMarketOpen !== undefined ? apiIsMarketOpen : isMarketOpenNow();

    if (isDelayed) {
        badge.className = "freshness-badge delayed";
        badge.innerText = "Yahoo Finance (EOD/Delayed) • 🟢 Verified Live NSE Filings";
        dot.className = "dot delayed";
        return;
    }

    if (!marketOpen) {
        badge.className = "freshness-badge market-closed";
        badge.innerText = "⚪ Market Closed (Closing Bell: 15:30 IST)";
        dot.className = "dot closed";

        if (lastSeenText) {
            lastSeenText.innerText = "Market closed. Session diff computed between your visit and final closing bell.";
        }
        return;
    }

    const lower = status ? status.toLowerCase() : "live";
    badge.className = `freshness-badge ${lower}`;
    badge.innerText = "🟢 Live Direct NSE Feed • Real-Time Ticks";
    dot.className = `dot ${lower}`;
}

function getZScoreExplanation(zScore) {
    if (zScore > 2.0) {
        return `🚨 High volatility alert: Price movement is <strong>${zScore.toFixed(2)}x standard deviations</strong> beyond its 5-minute rolling average.`;
    } else if (zScore >= 1.0) {
        return `⚡ Moderate volatility: Price is fluctuating <strong>${zScore.toFixed(2)}x standard deviations</strong> above baseline.`;
    } else {
        return `🌱 Low volatility: Price fluctuation is within normal rolling standard deviation bounds (<strong>${zScore.toFixed(2)}x</strong>).`;
    }
}

function getDemoData() {
    return {
        watchlistId: currentWatchlistId,
        anchorMode: currentAnchor,
        marketOpen: true,
        isDelayedFallback: false,
        lastSeenAt: new Date().toISOString(),
        synthesisSummary: "Demo Mode: 3 stocks broke momentum with abnormal volume led by TATAMOTORS (+2.4%) and RELIANCE (+1.6%), while 5 stocks remained quiet with minimal price churn. Market tone across your basket is strongly bullish (+0.81%).",
        activeMovers: [
            {
                symbol: "RELIANCE",
                companyName: "Reliance Industries Limited",
                currentPrice: 2895.00,
                anchorPrice: 2850.50,
                anchorTimeString: "09:15",
                deltaPercent: 1.56,
                compositeScore: 4.85,
                primaryDriver: "Volume Surge",
                catalystFlag: true,
                catalystBadgeText: "Dividend: Board Recommends Final Dividend of ₹10 per Share",
                newsUrl: "https://www.nseindia.com/get-quotes/equity?symbol=RELIANCE#announcements",
                crossoverBadge: "Golden Cross",
                volumeSurgeRatio: 2.45,
                volatilityZScore: 2.85,
                week52High: 3020.00,
                week52Low: 2220.00
            },
            {
                symbol: "TATAMOTORS",
                companyName: "Tata Motors Limited",
                currentPrice: 1003.75,
                anchorPrice: 980.25,
                anchorTimeString: "09:15",
                deltaPercent: 2.40,
                compositeScore: 4.20,
                primaryDriver: "Volume Surge",
                catalystFlag: true,
                catalystBadgeText: "Board Meeting: Board Meeting Scheduled for Q3 Results & EV Demerger",
                newsUrl: "https://www.nseindia.com/get-quotes/equity?symbol=TATAMOTORS#announcements",
                crossoverBadge: "Death Cross",
                volumeSurgeRatio: 2.10,
                volatilityZScore: 2.42,
                week52High: 1175.00,
                week52Low: 640.00
            },
            {
                symbol: "HDFCBANK",
                companyName: "HDFC Bank Limited",
                currentPrice: 1662.60,
                anchorPrice: 1640.00,
                anchorTimeString: "09:15",
                deltaPercent: 1.38,
                compositeScore: 3.95,
                primaryDriver: "Volume Surge",
                catalystFlag: true,
                catalystBadgeText: "Earnings: Q3 Net Interest Income Rises 16% YoY",
                newsUrl: "https://www.nseindia.com/get-quotes/equity?symbol=HDFCBANK#announcements",
                volumeSurgeRatio: 1.85,
                volatilityZScore: 2.10,
                week52High: 1794.00,
                week52Low: 1363.00
            }
        ],
        quietCount: 5,
        quietStocks: [
            {
                symbol: "TCS",
                companyName: "Tata Consultancy Services Limited",
                currentPrice: 4120.00,
                anchorPrice: 4101.50,
                anchorTimeString: "09:15",
                deltaPercent: 0.45,
                compositeScore: 2.85,
                primaryDriver: "Normal Churn",
                catalystFlag: false,
                catalystBadgeText: "Corporate Update: Official Corporate Filings & Announcements on NSE",
                newsUrl: "https://www.nseindia.com/get-quotes/equity?symbol=TCS#announcements",
                volumeSurgeRatio: 0.85,
                volatilityZScore: 0.65,
                week52High: 4585.00,
                week52Low: 3310.00
            },
            {
                symbol: "INFY",
                companyName: "Infosys Limited",
                currentPrice: 1820.00,
                anchorPrice: 1826.35,
                anchorTimeString: "09:15",
                deltaPercent: -0.35,
                compositeScore: 2.45,
                primaryDriver: "Normal Churn",
                catalystFlag: false,
                catalystBadgeText: "Corporate Update: Official Corporate Filings & Announcements on NSE",
                newsUrl: "https://www.nseindia.com/get-quotes/equity?symbol=INFY#announcements",
                crossoverBadge: "Death Cross",
                volumeSurgeRatio: 0.90,
                volatilityZScore: 0.42,
                week52High: 1990.00,
                week52Low: 1355.00
            },
            {
                symbol: "WIPRO",
                companyName: "Wipro Limited",
                currentPrice: 495.50,
                anchorPrice: 494.50,
                anchorTimeString: "09:15",
                deltaPercent: 0.20,
                compositeScore: 2.15,
                primaryDriver: "Normal Churn",
                catalystFlag: false,
                catalystBadgeText: "Corporate Update: Official Corporate Filings & Announcements on NSE",
                newsUrl: "https://www.nseindia.com/get-quotes/equity?symbol=WIPRO#announcements",
                volumeSurgeRatio: 0.75,
                volatilityZScore: 0.58,
                week52High: 545.00,
                week52Low: 375.00
            },
            {
                symbol: "SBIN",
                companyName: "State Bank of India",
                currentPrice: 815.00,
                anchorPrice: 810.90,
                anchorTimeString: "09:15",
                deltaPercent: 0.51,
                compositeScore: 2.65,
                primaryDriver: "Normal Churn",
                catalystFlag: false,
                catalystBadgeText: "Corporate Update: Official Corporate Filings & Announcements on NSE",
                newsUrl: "https://www.nseindia.com/get-quotes/equity?symbol=SBIN#announcements",
                volumeSurgeRatio: 0.95,
                volatilityZScore: 0.80,
                week52High: 912.00,
                week52Low: 560.00
            },
            {
                symbol: "ZOMATO",
                companyName: "Zomato Limited",
                currentPrice: 245.00,
                anchorPrice: 245.95,
                anchorTimeString: "09:15",
                deltaPercent: -0.39,
                compositeScore: 2.90,
                primaryDriver: "Normal Churn",
                catalystFlag: false,
                catalystBadgeText: "Corporate Update: Official Corporate Filings & Announcements on NSE",
                newsUrl: "https://www.nseindia.com/get-quotes/equity?symbol=ZOMATO#announcements",
                volumeSurgeRatio: 1.05,
                volatilityZScore: 0.92,
                week52High: 298.00,
                week52Low: 145.00
            }
        ]
    };
}

function sortStockList(list, sortBy) {
    if (!list || !Array.isArray(list)) return [];
    const sorted = [...list];
    switch (sortBy) {
        case "change":
            sorted.sort((a, b) => Math.abs(b.deltaPercent) - Math.abs(a.deltaPercent));
            break;
        case "volume":
            sorted.sort((a, b) => (b.volumeSurgeRatio || 0) - (a.volumeSurgeRatio || 0));
            break;
        case "name":
            sorted.sort((a, b) => (a.companyName || a.symbol).localeCompare(b.companyName || b.symbol));
            break;
        case "relevance":
        default:
            sorted.sort((a, b) => (b.compositeScore || 0) - (a.compositeScore || 0));
            break;
    }
    return sorted;
}

function openBuyModal(symbol, companyName, price, deltaPercent) {
    currentOrderSymbol = symbol;
    currentOrderCompany = companyName || symbol;
    currentOrderPrice = price;
    currentOrderDelta = deltaPercent;
    currentOrderType = "Delivery";

    const compEl = document.getElementById("buy-company-name");
    const symEl = document.getElementById("buy-symbol");
    const priceEl = document.getElementById("buy-price");
    const deltaEl = document.getElementById("buy-delta");

    if (compEl) compEl.innerText = currentOrderCompany;
    if (symEl) symEl.innerText = symbol;
    if (priceEl) priceEl.innerText = `₹${price.toFixed(2)}`;

    if (deltaEl) {
        const formattedDelta = (deltaPercent >= 0 ? "+" : "") + deltaPercent.toFixed(2) + "%";
        deltaEl.innerText = formattedDelta;
        deltaEl.className = deltaPercent >= 0 ? "modal-delta pos" : "modal-delta neg";
    }

    const qtyInput = document.getElementById("buy-qty");
    if (qtyInput) qtyInput.value = 1;

    const btnDeliv = document.getElementById("btn-delivery");
    const btnIntra = document.getElementById("btn-intraday");
    if (btnDeliv && btnIntra) {
        btnDeliv.className = "order-type-btn active";
        btnIntra.className = "order-type-btn";
    }

    updateModalTotal();

    const overlay = document.getElementById("buy-modal-overlay");
    if (overlay) overlay.classList.remove("hidden");
}

function closeBuyModal() {
    const overlay = document.getElementById("buy-modal-overlay");
    if (overlay) overlay.classList.add("hidden");
}

function updateModalTotal() {
    const qtyInput = document.getElementById("buy-qty");
    const totalEl = document.getElementById("buy-total-amount");
    if (!qtyInput || !totalEl) return;

    let qty = parseInt(qtyInput.value, 10);
    if (isNaN(qty) || qty < 1) qty = 1;
    qtyInput.value = qty;

    const total = qty * currentOrderPrice;
    totalEl.innerText = `₹${total.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function showToast(message) {
    const container = document.getElementById("toast-container");
    if (!container) return;

    const toast = document.createElement("div");
    toast.className = "groww-toast";
    toast.innerHTML = `
        <div class="toast-icon">✓</div>
        <div class="toast-message">${message}</div>
    `;

    container.appendChild(toast);
    setTimeout(() => {
        toast.classList.add("fade-out");
        setTimeout(() => toast.remove(), 400);
    }, 3500);
}

function getCategoryBadgeHtml(catalystBadgeText, newsUrl) {
    if (!catalystBadgeText || catalystBadgeText.trim().length === 0) {
        return `<a href="${newsUrl}" target="_blank" rel="noopener noreferrer" class="catalyst-link badge-general">📰 Read Official Corporate Filings & Announcements on NSE ↗</a>`;
    }

    const text = catalystBadgeText.trim();
    if (text.startsWith("Dividend")) {
        return `<a href="${newsUrl}" target="_blank" rel="noopener noreferrer" class="catalyst-link badge-dividend">💰 ${text} ↗</a>`;
    } else if (text.startsWith("Board Meeting")) {
        return `<a href="${newsUrl}" target="_blank" rel="noopener noreferrer" class="catalyst-link badge-board">🏛️ ${text} ↗</a>`;
    } else if (text.startsWith("Earnings")) {
        return `<a href="${newsUrl}" target="_blank" rel="noopener noreferrer" class="catalyst-link badge-earnings">📈 ${text} ↗</a>`;
    } else if (text.startsWith("M&A / Partnership")) {
        return `<a href="${newsUrl}" target="_blank" rel="noopener noreferrer" class="catalyst-link badge-ma">🤝 ${text} ↗</a>`;
    } else if (text.startsWith("Contract Win")) {
        return `<a href="${newsUrl}" target="_blank" rel="noopener noreferrer" class="catalyst-link badge-contract">📜 ${text} ↗</a>`;
    } else if (text.startsWith("Management")) {
        return `<a href="${newsUrl}" target="_blank" rel="noopener noreferrer" class="catalyst-link badge-management">👔 ${text} ↗</a>`;
    } else {
        return `<a href="${newsUrl}" target="_blank" rel="noopener noreferrer" class="catalyst-link badge-filing">📑 ${text} ↗</a>`;
    }
}

function renderStockRowHTML(item, isQuiet) {
    const formattedDelta = (item.deltaPercent > 0 ? "+" : "") + item.deltaPercent.toFixed(2) + "%";
    const deltaClass = item.deltaPercent > 0 ? "delta-positive" : (item.deltaPercent < 0 ? "delta-negative" : "delta-neutral");
    const catalystHtml = getCategoryBadgeHtml(item.catalystBadgeText, item.newsUrl);
    
    let crossoverHtml = "";
    if (item.crossoverBadge) {
        const badge = item.crossoverBadge.toUpperCase();
        if (badge.includes("GOLDEN")) {
            crossoverHtml = `<span class="badge-crossover golden">✨ Golden Cross</span>`;
        } else if (badge.includes("DEATH")) {
            crossoverHtml = `<span class="badge-crossover death">💀 Death Cross</span>`;
        }
    }

    const volSurge = item.volumeSurgeRatio ? item.volumeSurgeRatio.toFixed(1) : "1.0";
    const zScore = item.volatilityZScore ? item.volatilityZScore.toFixed(2) : "0.00";
    const low52 = item.week52Low ? item.week52Low : item.currentPrice * 0.85;
    const high52 = item.week52High ? item.week52High : item.currentPrice * 1.15;

    let rangePercent = 50;
    if (high52 > low52) {
        rangePercent = Math.min(100, Math.max(0, ((item.currentPrice - low52) / (high52 - low52)) * 100));
    }

    const zExplanation = getZScoreExplanation(item.volatilityZScore || 0);
    const anchorPriceStr = item.anchorPrice ? `₹${item.anchorPrice.toFixed(2)}` : "—";
    const anchorTimeStr = item.anchorTimeString ? item.anchorTimeString : "";
    const escapedComp = (item.companyName || item.symbol).replace(/'/g, "\\'");

    const isExpanded = expandedStockSymbols.has(item.symbol);
    const drawerClass = isExpanded ? "stock-detail-drawer expanded" : "stock-detail-drawer";

    return `
        <div class="mover-card">
            <div class="mover-top">
                <div class="mover-info" onclick="toggleStockDrawer(this, '${item.symbol}')">
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <span class="symbol">${item.symbol}</span>
                        <button class="btn-remove-stock" onclick="event.stopPropagation(); removeStockFromWatchlist('${item.symbol}')" title="Remove ${item.symbol}">✕</button>
                    </div>
                    <span class="company-name">${item.companyName || item.symbol}</span>
                </div>
                <div class="mover-price-box">
                    <div class="price-row">
                        <span class="price">₹${item.currentPrice.toFixed(2)}</span>
                        <span class="delta-badge ${deltaClass}">${formattedDelta}</span>
                    </div>
                    <span class="baseline-comparison">Was ${anchorPriceStr} at ${anchorTimeStr}</span>
                </div>
            </div>

            <div class="mover-action-row">
                <div class="metrics-bar" onclick="toggleStockDrawer(this, '${item.symbol}')" style="cursor: pointer;">
                    <span class="chip chip-score">Score: ${item.compositeScore.toFixed(2)}</span>
                    <span class="chip chip-volume">⚡ ${volSurge}x Vol</span>
                    <span class="chip chip-zscore">📊 Z: ${zScore}</span>
                    <span class="chip chip-range">52W Range ▾</span>
                </div>
                ${!isQuiet ? `
                <div class="trade-actions" onclick="event.stopPropagation()">
                    <button class="btn-trade-buy" onclick="openBuyModal('${item.symbol}', '${escapedComp}', ${item.currentPrice}, ${item.deltaPercent})">
                        + Quick Buy
                    </button>
                    <a href="${item.newsUrl}" target="_blank" rel="noopener noreferrer" class="btn-trade-chart">
                        📈 Chart
                    </a>
                </div>` : ''}
            </div>

            <div class="catalyst-box" style="margin-top: 6px; display: flex; align-items: center; flex-wrap: wrap; gap: 6px;">
                ${catalystHtml}
                ${crossoverHtml}
            </div>

            <!-- Expandable Stock Detail Drawer -->
            <div class="${drawerClass}" onclick="event.stopPropagation()">
                <div class="drawer-grid">
                    <div class="drawer-stat">
                        <span class="drawer-stat-label">Current Price</span>
                        <span class="drawer-stat-value">₹${item.currentPrice.toFixed(2)}</span>
                    </div>
                    <div class="drawer-stat">
                        <span class="drawer-stat-label">Volume Surge Ratio</span>
                        <span class="drawer-stat-value">${volSurge}x vs 20D Avg</span>
                    </div>
                    <div class="drawer-stat">
                        <span class="drawer-stat-label">Volatility Z-Score</span>
                        <span class="drawer-stat-value">${zScore}</span>
                    </div>
                    <div class="drawer-stat">
                        <span class="drawer-stat-label">Composite Score</span>
                        <span class="drawer-stat-value">${item.compositeScore.toFixed(2)} / 5.0</span>
                    </div>

                    <!-- 52-Week Visual Range Progress Bar -->
                    <div class="range-bar-box">
                        <div class="range-bar-header">
                            <span>52W Low: ₹${low52.toFixed(2)}</span>
                            <span class="range-current-badge">Current: ₹${item.currentPrice.toFixed(2)}</span>
                            <span>52W High: ₹${high52.toFixed(2)}</span>
                        </div>
                        <div class="range-track">
                            <div class="range-fill" style="width: ${rangePercent.toFixed(1)}%;"></div>
                            <div class="range-marker" style="left: ${rangePercent.toFixed(1)}%;"></div>
                        </div>
                    </div>
                </div>

                <div class="zscore-explanation">
                    ${zExplanation}
                </div>
            </div>
        </div>
    `;
}

let expandedStockSymbols = new Set();

function toggleStockDrawer(element, symbol) {
    const cardElement = element.closest ? element.closest(".mover-card") : element;
    if (!cardElement) return;
    const drawer = cardElement.querySelector(".stock-detail-drawer");
    if (drawer) {
        const isExpanded = drawer.classList.toggle("expanded");
        if (symbol) {
            if (isExpanded) {
                expandedStockSymbols.add(symbol);
            } else {
                expandedStockSymbols.delete(symbol);
            }
        }
    }
}

function renderCatchUpUI() {
    if (!rawCatchUpData) return;

    const moversList = document.getElementById("movers-list");
    const quietBtnText = document.getElementById("quiet-count-text");
    const activeCount = document.getElementById("active-count");
    const summaryActiveCount = document.getElementById("summary-active-count");
    const summaryQuietCount = document.getElementById("summary-quiet-count");
    const quietList = document.getElementById("quiet-list");
    const lastSeenText = document.getElementById("last-seen-text");
    const synthesisText = document.getElementById("synthesis-text");

    if (!moversList) return;

    const data = rawCatchUpData;
    const isDelayed = data.isDelayedFallback || data.delayedFallback;
    const marketOpen = data.marketOpen !== undefined ? data.marketOpen : data.isMarketOpen;

    setFreshness("Live", isDelayed, marketOpen);

    // Render Stale / Delayed Data / Market Closed Alert Banner
    const alertContainer = document.getElementById("data-alert-container");
    if (alertContainer) {
        alertContainer.innerHTML = "";
        if (isDemoMode) {
            alertContainer.innerHTML = `
                <div class="market-closed-alert-banner" style="background-color: #f5f3ff; border-color: #ddd6fe; border-left-color: #7c3aed; color: #5b21b6;">
                    <span class="alert-icon">🎮</span>
                    <div><strong>Demo Mode Active:</strong> Displaying deterministic simulated market data for reproducible testing.</div>
                </div>`;
        } else if (isDelayed) {
            alertContainer.innerHTML = `
                <div class="stale-data-alert-banner">
                    <span class="alert-icon">⚠️</span>
                    <div><strong>Data Alert — Serving Delayed/EOD Fallback Feed:</strong> Live direct NSE streaming feed is offline or rate-limited. Currently serving Yahoo Finance daily chart closes paired with live official NSE corporate announcements.</div>
                </div>`;
        } else if (!marketOpen) {
            alertContainer.innerHTML = `
                <div class="market-closed-alert-banner">
                    <span class="alert-icon">🌙</span>
                    <div><strong>Market Closed Notice:</strong> Markets are closed (Last closing bell: 15:30 IST). Serving frozen 15:30 close prices with zero off-hour false diff jitter. Diffs reflect momentum up to the closing bell.</div>
                </div>`;
        }
    }

    if (lastSeenText && data.lastSeenAt && !isDemoMode) {
        const dateObj = new Date(data.lastSeenAt);
        const marketOpen = data.marketOpen !== undefined ? data.marketOpen : data.isMarketOpen;
        if (marketOpen) {
            lastSeenText.innerText = currentAnchor === "SINCE_LAST_SEEN"
                ? `Diff computed relative to your last visit at ${dateObj.toLocaleTimeString()}`
                : `Diff computed relative to today's NSE market open baseline`;
        }
    }

    if (synthesisText && data.synthesisSummary) {
        synthesisText.innerText = data.synthesisSummary;
    }

    const rawMovers = data.activeMovers || [];
    const quietCount = data.quietCount || (data.quietStocks ? data.quietStocks.length : 0);
    const rawQuietStocks = data.quietStocks || [];

    const movers = sortStockList(rawMovers, currentSortBy);
    const quietStocks = sortStockList(rawQuietStocks, currentSortBy);

    // Update Top Banner Summary Cards
    if (activeCount) activeCount.innerText = movers.length;
    if (summaryActiveCount) summaryActiveCount.innerText = movers.length;
    if (summaryQuietCount) summaryQuietCount.innerText = quietCount;

    // Render Active Movers / Empty State
    moversList.innerHTML = "";
    if (movers.length === 0 && quietStocks.length === 0) {
        moversList.innerHTML = `
            <div class="empty-watchlist-card">
                <div class="empty-icon">📂</div>
                <h3>This Watchlist is Empty</h3>
                <p>Start tracking session price diffs, volume surges, and catalyst events by searching for NSE stocks or picking a curated basket.</p>
                <div class="empty-quick-add">
                    <span>Quick Add Top Constituents:</span>
                    <div class="quick-chips">
                        <button onclick="addStockToWatchlist('RELIANCE')" class="btn-quick-chip">+ RELIANCE</button>
                        <button onclick="addStockToWatchlist('TCS')" class="btn-quick-chip">+ TCS</button>
                        <button onclick="addStockToWatchlist('INFY')" class="btn-quick-chip">+ INFY</button>
                        <button onclick="addStockToWatchlist('TATAMOTORS')" class="btn-quick-chip">+ TATAMOTORS</button>
                        <button onclick="addStockToWatchlist('HDFCBANK')" class="btn-quick-chip">+ HDFCBANK</button>
                    </div>
                </div>
            </div>`;
    } else if (movers.length === 0) {
        moversList.innerHTML = `
            <div class="mover-card" style="color: var(--text-muted); font-size: 13px; text-align: center; padding: 24px;">
                Your watchlist has no active movers above volatility threshold. Use the search bar above to add any NSE stock!
            </div>`;
    } else {
        movers.forEach(mover => {
            const wrap = document.createElement("div");
            wrap.innerHTML = renderStockRowHTML(mover, false);
            moversList.appendChild(wrap.firstElementChild);
        });
    }

    // Render Quiet Stocks
    if (quietBtnText) {
        quietBtnText.innerText = `▾ Quiet Stocks (${quietCount}) — Normal Churn`;
    }

    if (quietList) {
        quietList.innerHTML = "";
        if (quietStocks.length === 0 && movers.length > 0) {
            quietList.innerHTML = `<p class="quiet-note">No quiet stocks currently receding.</p>`;
        } else if (quietStocks.length > 0) {
            quietStocks.forEach(qs => {
                const wrap = document.createElement("div");
                wrap.innerHTML = renderStockRowHTML(qs, true);
                quietList.appendChild(wrap.firstElementChild);
            });
        }
    }
}

async function loadCatchUpCard() {
    if (isDemoMode) {
        rawCatchUpData = getDemoData();
        renderCatchUpUI();
        return;
    }

    try {
        const res = await fetch(`/api/watchlists/${currentWatchlistId}/delta?anchor=${currentAnchor}`);
        if (!res.ok) {
            setFreshness("Delayed", true);
            return;
        }
        rawCatchUpData = await res.json();
        renderCatchUpUI();
    } catch (e) {
        setFreshness("Reconnecting", false);
    }
}

async function addStockToWatchlist(symbol) {
    if (!symbol) return;
    try {
        const res = await fetch(`/api/watchlists/${currentWatchlistId}/stocks`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ symbol: symbol.trim().toUpperCase() })
        });
        if (res.ok) {
            const input = document.getElementById("add-stock-input");
            const dropdown = document.getElementById("search-dropdown");
            if (input) input.value = "";
            if (dropdown) dropdown.classList.add("hidden");
            await loadCatchUpCard();
        }
    } catch (e) {
        // silent handling
    }
}

async function removeStockFromWatchlist(symbol) {
    if (!symbol) return;
    try {
        const res = await fetch(`/api/watchlists/${currentWatchlistId}/stocks/${encodeURIComponent(symbol)}`, {
            method: "DELETE"
        });
        if (res.ok) {
            showToast(`Removed <strong>${symbol}</strong> from watchlist`);
            await loadCatchUpCard();
        }
    } catch (e) {
        console.error("Error removing stock:", e);
    }
}

async function createNewWatchlist(name) {
    if (!name || name.trim().length === 0) return;
    try {
        const res = await fetch(`/api/watchlists`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ userId: 1, name: name.trim() })
        });
        if (res.ok) {
            const newList = await res.json();
            currentWatchlistId = newList.id;
            const nameInput = document.getElementById("new-watchlist-name");
            if (nameInput) nameInput.value = "";
            showToast(`Created watchlist <strong>"${newList.name}"</strong>`);
            await loadUserWatchlists();
            await loadCatchUpCard();
        }
    } catch (e) {
        console.error("Error creating watchlist:", e);
    }
}

async function deleteCurrentWatchlist() {
    if (currentWatchlistId === 1) {
        showToast("Primary default watchlist cannot be deleted.");
        return;
    }
    if (!confirm("Are you sure you want to delete this custom watchlist?")) return;

    try {
        const res = await fetch(`/api/watchlists/${currentWatchlistId}`, {
            method: "DELETE"
        });
        if (res.ok) {
            showToast("Watchlist deleted.");
            currentWatchlistId = 1;
            await loadUserWatchlists();
            await loadCatchUpCard();
        }
    } catch (e) {
        console.error("Error deleting watchlist:", e);
    }
}

async function loadUserWatchlists() {
    const select = document.getElementById("watchlist-select");
    const deleteBtn = document.getElementById("delete-watchlist-btn");
    if (!select) return;

    try {
        const res = await fetch("/api/watchlists");
        if (!res.ok) return;
        const watchlists = await res.json();

        select.innerHTML = "";
        watchlists.forEach(wl => {
            const opt = document.createElement("option");
            opt.value = wl.id;
            opt.innerText = wl.name;
            if (wl.id === currentWatchlistId) opt.selected = true;
            select.appendChild(opt);
        });

        if (deleteBtn) {
            deleteBtn.style.display = (currentWatchlistId !== 1) ? "block" : "none";
        }
    } catch (e) {
        console.error("Error loading watchlists:", e);
    }
}

async function loadBaskets() {
    const container = document.getElementById("baskets-container");
    if (!container) return;

    try {
        const res = await fetch("/api/baskets");
        if (!res.ok) return;
        const baskets = await res.json();

        container.innerHTML = "";
        baskets.forEach(basket => {
            const div = document.createElement("div");
            div.className = "basket-item";
            div.innerHTML = `
                <div class="basket-name">${basket.name}</div>
                <div class="basket-desc">${basket.description}</div>
            `;
            div.addEventListener("click", async () => {
                try {
                    const batchRes = await fetch(`/api/watchlists/${currentWatchlistId}/basket`, {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ symbols: basket.symbols })
                    });
                    if (batchRes.ok) {
                        showToast(`Loaded <strong>${basket.name}</strong> into Catch-Up diff engine (${basket.symbols.length} stocks)`);
                        await loadCatchUpCard();
                    }
                } catch (e) {
                    // silent handling
                }
            });
            container.appendChild(div);
        });
    } catch (e) {
        console.error("Error loading baskets:", e);
    }
}

let searchDebounce = null;
async function performSearch(query) {
    const dropdown = document.getElementById("search-dropdown");
    if (!dropdown) return;

    if (!query || query.trim().length === 0) {
        dropdown.classList.add("hidden");
        return;
    }

    try {
        const res = await fetch(`/api/stocks/search?q=${encodeURIComponent(query.trim())}`);
        if (!res.ok) return;
        const results = await res.json();

        if (results.length === 0) {
            dropdown.classList.add("hidden");
            return;
        }

        dropdown.innerHTML = "";
        results.forEach(item => {
            const div = document.createElement("div");
            div.className = "dropdown-item";
            div.innerHTML = `
                <span class="dropdown-symbol">${item.symbol}</span>
                <span class="dropdown-name">${item.companyName}</span>
            `;
            div.addEventListener("click", () => {
                addStockToWatchlist(item.symbol);
            });
            dropdown.appendChild(div);
        });
        dropdown.classList.remove("hidden");

    } catch (e) {
        // silent handling
    }
}

document.addEventListener("DOMContentLoaded", () => {
    loadUserWatchlists();
    loadCatchUpCard();
    loadBaskets();

    // Auto refresh timer
    if (pollTimer) clearInterval(pollTimer);
    pollTimer = setInterval(loadCatchUpCard, 4000);

    // Watchlist Select Listener
    const watchlistSelect = document.getElementById("watchlist-select");
    if (watchlistSelect) {
        watchlistSelect.addEventListener("change", (e) => {
            currentWatchlistId = parseInt(e.target.value, 10);
            const deleteBtn = document.getElementById("delete-watchlist-btn");
            if (deleteBtn) {
                deleteBtn.style.display = (currentWatchlistId !== 1) ? "block" : "none";
            }
            loadCatchUpCard();
        });
    }

    // Delete Watchlist Listener
    const deleteWlBtn = document.getElementById("delete-watchlist-btn");
    if (deleteWlBtn) {
        deleteWlBtn.addEventListener("click", deleteCurrentWatchlist);
    }

    // Demo Mode Toggle Listener
    const demoToggle = document.getElementById("demo-mode-toggle");
    if (demoToggle) {
        demoToggle.addEventListener("change", (e) => {
            isDemoMode = e.target.checked;
            loadCatchUpCard();
        });
    }

    // Sort Dropdown Listener
    const sortSelect = document.getElementById("sort-select");
    if (sortSelect) {
        sortSelect.addEventListener("change", (e) => {
            currentSortBy = e.target.value;
            renderCatchUpUI();
        });
    }

    // Search Autocomplete Events
    const addInput = document.getElementById("add-stock-input");
    const addBtn = document.getElementById("add-stock-btn");

    if (addInput) {
        addInput.addEventListener("input", (e) => {
            clearTimeout(searchDebounce);
            searchDebounce = setTimeout(() => performSearch(e.target.value), 200);
        });

        addInput.addEventListener("keypress", (e) => {
            if (e.key === "Enter") {
                addStockToWatchlist(addInput.value);
            }
        });
    }

    if (addBtn && addInput) {
        addBtn.addEventListener("click", () => addStockToWatchlist(addInput.value));
    }

    // Close dropdown on click outside
    document.addEventListener("click", (e) => {
        const dropdown = document.getElementById("search-dropdown");
        const wrapper = document.querySelector(".search-wrapper");
        if (dropdown && wrapper && !wrapper.contains(e.target)) {
            dropdown.classList.add("hidden");
        }
    });

    // Anchor Mode Buttons
    const btnLastSeen = document.getElementById("btn-last-seen");
    const btnSinceOpen = document.getElementById("btn-since-open");

    if (btnLastSeen && btnSinceOpen) {
        btnLastSeen.addEventListener("click", () => {
            currentAnchor = "SINCE_LAST_SEEN";
            btnLastSeen.classList.add("active");
            btnSinceOpen.classList.remove("active");
            loadCatchUpCard();
        });

        btnSinceOpen.addEventListener("click", () => {
            currentAnchor = "SINCE_OPEN";
            btnSinceOpen.classList.add("active");
            btnLastSeen.classList.remove("active");
            loadCatchUpCard();
        });
    }

    // Create Watchlist
    const createBtn = document.getElementById("create-watchlist-btn");
    const nameInput = document.getElementById("new-watchlist-name");
    if (createBtn && nameInput) {
        createBtn.addEventListener("click", () => {
            createNewWatchlist(nameInput.value);
        });
    }

    // Quiet stocks accordion toggle
    const quietToggleBtn = document.getElementById("quiet-toggle-btn");
    const quietList = document.getElementById("quiet-list");

    if (quietToggleBtn && quietList) {
        quietToggleBtn.addEventListener("click", () => {
            quietList.classList.toggle("open");
            quietToggleBtn.classList.toggle("open");
        });
    }

    // Buy Modal Events
    const closeModalBtn = document.getElementById("buy-modal-close");
    const overlay = document.getElementById("buy-modal-overlay");

    if (closeModalBtn) closeModalBtn.addEventListener("click", closeBuyModal);
    if (overlay) {
        overlay.addEventListener("click", (e) => {
            if (e.target === overlay) closeBuyModal();
        });
    }

    const btnDeliv = document.getElementById("btn-delivery");
    const btnIntra = document.getElementById("btn-intraday");

    if (btnDeliv && btnIntra) {
        btnDeliv.addEventListener("click", () => {
            currentOrderType = "Delivery";
            btnDeliv.classList.add("active");
            btnIntra.classList.remove("active");
        });
        btnIntra.addEventListener("click", () => {
            currentOrderType = "Intraday";
            btnIntra.classList.add("active");
            btnDeliv.classList.remove("active");
        });
    }

    const qtyInput = document.getElementById("buy-qty");
    const btnMinus = document.getElementById("qty-minus");
    const btnPlus = document.getElementById("qty-plus");

    if (qtyInput) {
        qtyInput.addEventListener("input", updateModalTotal);
    }
    if (btnMinus && qtyInput) {
        btnMinus.addEventListener("click", () => {
            let val = parseInt(qtyInput.value, 10) || 1;
            if (val > 1) {
                qtyInput.value = val - 1;
                updateModalTotal();
            }
        });
    }
    if (btnPlus && qtyInput) {
        btnPlus.addEventListener("click", () => {
            let val = parseInt(qtyInput.value, 10) || 1;
            qtyInput.value = val + 1;
            updateModalTotal();
        });
    }

    const btnSubmit = document.getElementById("btn-place-order");
    if (btnSubmit) {
        btnSubmit.addEventListener("click", () => {
            const qty = parseInt(document.getElementById("buy-qty").value, 10) || 1;
            const totalStr = document.getElementById("buy-total-amount").innerText;
            closeBuyModal();
            showToast(`Placed <strong>${currentOrderType} Buy Order</strong> for ${qty} share(s) of <strong>${currentOrderSymbol}</strong> (${totalStr})`);
        });
    }
});
