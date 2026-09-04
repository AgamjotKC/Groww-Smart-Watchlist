let currentAnchor = "SINCE_LAST_SEEN";
let currentWatchlistId = 1;
let pollTimer = null;

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

    const marketOpen = apiIsMarketOpen !== undefined ? apiIsMarketOpen : isMarketOpenNow();

    if (isDelayed) {
        badge.className = "freshness-badge delayed";
        badge.innerText = "Delayed (Cached)";
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
    badge.innerText = "🟢 Market Open • Live NSE Feed";
    dot.className = `dot ${lower}`;
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
    } else {
        return `<a href="${newsUrl}" target="_blank" rel="noopener noreferrer" class="catalyst-link badge-filing">📑 ${text} ↗</a>`;
    }
}

async function loadCatchUpCard() {
    const moversList = document.getElementById("movers-list");
    const quietBtnText = document.getElementById("quiet-count-text");
    const activeCount = document.getElementById("active-count");
    const summaryActiveCount = document.getElementById("summary-active-count");
    const summaryQuietCount = document.getElementById("summary-quiet-count");
    const quietList = document.getElementById("quiet-list");
    const lastSeenText = document.getElementById("last-seen-text");
    const synthesisText = document.getElementById("synthesis-text");

    if (!moversList) return;

    try {
        const res = await fetch(`/api/watchlists/${currentWatchlistId}/delta?anchor=${currentAnchor}`);
        if (!res.ok) {
            setFreshness("Delayed", true);
            return;
        }
        const data = await res.json();

        setFreshness("Live", data.isDelayedFallback || data.delayedFallback, data.marketOpen !== undefined ? data.marketOpen : data.isMarketOpen);

        if (lastSeenText && data.lastSeenAt) {
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

        const movers = data.activeMovers || [];
        const quietCount = data.quietCount || 0;
        const quietStocks = data.quietStocks || [];

        // Update Top Banner Summary Cards
        if (activeCount) activeCount.innerText = movers.length;
        if (summaryActiveCount) summaryActiveCount.innerText = movers.length;
        if (summaryQuietCount) summaryQuietCount.innerText = quietCount;

        // Render Active Movers with All 4 Metrics + Categorized Badges + Baseline Comparison + Action Sheet
        moversList.innerHTML = "";
        if (movers.length === 0) {
            moversList.innerHTML = `
                <div class="mover-card" style="color: var(--text-muted); font-size: 13px; text-align: center; padding: 24px;">
                    Your watchlist has no active movers above volatility threshold. Use the search bar above to add any NSE stock!
                </div>`;
        } else {
            movers.forEach(mover => {
                const card = document.createElement("div");
                card.className = "mover-card";

                const formattedDelta = (mover.deltaPercent > 0 ? "+" : "") + mover.deltaPercent.toFixed(2) + "%";
                const deltaClass = mover.deltaPercent > 0 ? "delta-positive" : (mover.deltaPercent < 0 ? "delta-negative" : "delta-neutral");

                const companySubtitle = mover.companyName && mover.companyName !== mover.symbol
                    ? `<span class="company-name">${mover.companyName}</span>`
                    : `<span class="company-name">${mover.symbol} Equities</span>`;

                const catalystHtml = getCategoryBadgeHtml(mover.catalystBadgeText, mover.newsUrl);

                const volSurge = mover.volumeSurgeRatio ? mover.volumeSurgeRatio.toFixed(1) : "1.0";
                const zScore = mover.volatilityZScore ? mover.volatilityZScore.toFixed(2) : "0.00";
                const low52 = mover.week52Low ? Math.round(mover.week52Low) : Math.round(mover.currentPrice * 0.85);
                const high52 = mover.week52High ? Math.round(mover.week52High) : Math.round(mover.currentPrice * 1.15);

                const anchorPriceStr = mover.anchorPrice ? `₹${mover.anchorPrice.toFixed(2)}` : "—";
                const anchorTimeStr = mover.anchorTimeString ? mover.anchorTimeString : "";
                const baselineComparisonText = `Was ${anchorPriceStr} at ${anchorTimeStr}`;
                const escapedComp = (mover.companyName || mover.symbol).replace(/'/g, "\\'");

                card.innerHTML = `
                    <div class="mover-top">
                        <div class="mover-info">
                            <span class="symbol">${mover.symbol}</span>
                            ${companySubtitle}
                        </div>
                        <div class="mover-price-box">
                            <div class="price-row">
                                <span class="price">₹${mover.currentPrice.toFixed(2)}</span>
                                <span class="delta-badge ${deltaClass}">${formattedDelta}</span>
                            </div>
                            <span class="baseline-comparison">${baselineComparisonText}</span>
                        </div>
                    </div>

                    <div class="mover-action-row">
                        <div class="metrics-bar">
                            <span class="chip chip-score">Score: ${mover.compositeScore.toFixed(2)}</span>
                            <span class="chip chip-volume">⚡ ${volSurge}x Vol</span>
                            <span class="chip chip-zscore">📊 Z: ${zScore}</span>
                            <span class="chip chip-range">52W: ₹${low52} - ₹${high52}</span>
                        </div>
                        <div class="trade-actions">
                            <button class="btn-trade-buy" onclick="openBuyModal('${mover.symbol}', '${escapedComp}', ${mover.currentPrice}, ${mover.deltaPercent})">
                                + Quick Buy
                            </button>
                            <a href="${mover.newsUrl}" target="_blank" rel="noopener noreferrer" class="btn-trade-chart">
                                📈 Chart
                            </a>
                        </div>
                    </div>

                    <div class="catalyst-box">
                        ${catalystHtml}
                    </div>
                `;
                moversList.appendChild(card);
            });
        }

        // Render Quiet Stocks with All 4 Metrics + Accordion Label
        if (quietBtnText) {
            quietBtnText.innerText = `▾ Quiet Stocks (${quietCount}) — Normal Churn`;
        }

        if (quietList) {
            quietList.innerHTML = "";
            if (quietStocks.length === 0) {
                quietList.innerHTML = `<p class="quiet-note">No quiet stocks currently receding.</p>`;
            } else {
                quietStocks.forEach(qs => {
                    const qCard = document.createElement("div");
                    qCard.className = "quiet-card";
                    const formattedDelta = (qs.deltaPercent > 0 ? "+" : "") + qs.deltaPercent.toFixed(2) + "%";
                    const deltaClass = qs.deltaPercent > 0 ? "delta-positive" : (qs.deltaPercent < 0 ? "delta-negative" : "delta-neutral");

                    const volSurge = qs.volumeSurgeRatio ? qs.volumeSurgeRatio.toFixed(1) : "1.0";
                    const zScore = qs.volatilityZScore ? qs.volatilityZScore.toFixed(2) : "0.00";
                    const low52 = qs.week52Low ? Math.round(qs.week52Low) : Math.round(qs.currentPrice * 0.85);
                    const high52 = qs.week52High ? Math.round(qs.week52High) : Math.round(qs.currentPrice * 1.15);

                    const catalystHtml = getCategoryBadgeHtml(qs.catalystBadgeText, qs.newsUrl);
                    const anchorPriceStr = qs.anchorPrice ? `₹${qs.anchorPrice.toFixed(2)}` : "—";
                    const anchorTimeStr = qs.anchorTimeString ? qs.anchorTimeString : "";

                    qCard.innerHTML = `
                        <div class="mover-top">
                            <div class="mover-info">
                                <span class="symbol">${qs.symbol}</span>
                                <span class="company-name">${qs.companyName || qs.symbol}</span>
                            </div>
                            <div class="mover-price-box">
                                <div class="price-row">
                                    <span class="price">₹${qs.currentPrice.toFixed(2)}</span>
                                    <span class="delta-badge ${deltaClass}">${formattedDelta}</span>
                                </div>
                                <span class="baseline-comparison">Was ${anchorPriceStr} at ${anchorTimeStr}</span>
                            </div>
                        </div>

                        <div class="metrics-bar" style="margin-top: 4px;">
                            <span class="chip chip-score">Score: ${qs.compositeScore.toFixed(2)}</span>
                            <span class="chip chip-volume">⚡ ${volSurge}x Vol</span>
                            <span class="chip chip-zscore">📊 Z: ${zScore}</span>
                            <span class="chip chip-range">52W: ₹${low52} - ₹${high52}</span>
                        </div>

                        <div class="catalyst-box" style="margin-top: 4px;">
                            ${catalystHtml}
                        </div>
                    `;
                    quietList.appendChild(qCard);
                });
            }
        }

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

async function createNewWatchlist(name) {
    if (!name) return;
    try {
        const res = await fetch(`/api/watchlists`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ userId: 1, name: name.trim() })
        });
        if (res.ok) {
            const newList = await res.json();
            currentWatchlistId = newList.id;
            
            const select = document.getElementById("watchlist-select");
            if (select) {
                const opt = document.createElement("option");
                opt.value = newList.id;
                opt.innerText = newList.name;
                opt.selected = true;
                select.appendChild(opt);
            }
            const nameInput = document.getElementById("new-watchlist-name");
            if (nameInput) nameInput.value = "";
            await loadCatchUpCard();
        }
    } catch (e) {
        // silent handling
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
                for (const sym of basket.symbols) {
                    await addStockToWatchlist(sym);
                }
            });
            container.appendChild(div);
        });
    } catch (e) {
        // silent handling
    }
}

// Live Autocomplete Search Dropdown
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
    loadCatchUpCard();
    loadBaskets();

    // Auto refresh timer
    if (pollTimer) clearInterval(pollTimer);
    pollTimer = setInterval(loadCatchUpCard, 4000);

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

    // Create Watchlist Controls
    const createBtn = document.getElementById("create-watchlist-btn");
    const createInput = document.getElementById("new-watchlist-name");

    if (createBtn && createInput) {
        createBtn.addEventListener("click", () => createNewWatchlist(createInput.value));
        createInput.addEventListener("keypress", (e) => {
            if (e.key === "Enter") createNewWatchlist(createInput.value);
        });
    }

    // Watchlist Dropdown Switch
    const select = document.getElementById("watchlist-select");
    if (select) {
        select.addEventListener("change", (e) => {
            currentWatchlistId = parseInt(e.target.value, 10);
            loadCatchUpCard();
        });
    }

    // Quiet Drawer Toggle
    const quietBtn = document.getElementById("quiet-toggle-btn");
    const quietContent = document.getElementById("quiet-content");
    if (quietBtn && quietContent) {
        quietBtn.addEventListener("click", () => {
            quietContent.classList.toggle("hidden");
            quietBtn.classList.toggle("expanded");
        });
    }

    // Quick Buy Modal Event Listeners
    const modalClose = document.getElementById("modal-close-btn");
    const overlay = document.getElementById("buy-modal-overlay");
    const qtyMinus = document.getElementById("qty-minus");
    const qtyPlus = document.getElementById("qty-plus");
    const qtyInput = document.getElementById("buy-qty");
    const btnDeliv = document.getElementById("btn-delivery");
    const btnIntra = document.getElementById("btn-intraday");
    const btnPlaceOrder = document.getElementById("btn-place-order");

    if (modalClose) modalClose.addEventListener("click", closeBuyModal);

    if (overlay) {
        overlay.addEventListener("click", (e) => {
            if (e.target === overlay) closeBuyModal();
        });
    }

    if (qtyMinus && qtyInput) {
        qtyMinus.addEventListener("click", () => {
            let qty = parseInt(qtyInput.value, 10) || 1;
            if (qty > 1) {
                qtyInput.value = qty - 1;
                updateModalTotal();
            }
        });
    }

    if (qtyPlus && qtyInput) {
        qtyPlus.addEventListener("click", () => {
            let qty = parseInt(qtyInput.value, 10) || 1;
            qtyInput.value = qty + 1;
            updateModalTotal();
        });
    }

    if (qtyInput) {
        qtyInput.addEventListener("input", updateModalTotal);
    }

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

    if (btnPlaceOrder) {
        btnPlaceOrder.addEventListener("click", () => {
            const qty = parseInt(qtyInput ? qtyInput.value : 1, 10) || 1;
            const msg = `Order simulated successfully for <strong>${currentOrderSymbol}</strong> (${qty} Qty @ ₹${currentOrderPrice.toFixed(2)} - ${currentOrderType})`;
            closeBuyModal();
            showToast(msg);
        });
    }
});
