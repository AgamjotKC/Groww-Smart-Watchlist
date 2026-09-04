let currentAnchor = "SINCE_LAST_SEEN";
let currentWatchlistId = 1;
let pollTimer = null;

function setFreshness(status) {
    const badge = document.getElementById("freshness-badge");
    const dot = document.getElementById("freshness-dot");
    if (!badge || !dot) return;

    const lower = status.toLowerCase();
    badge.className = `freshness-badge ${lower}`;
    badge.innerText = status === "Live" ? "Live NSE Feed" : status;

    dot.className = `dot ${lower}`;
}

async function loadCatchUpCard() {
    const moversList = document.getElementById("movers-list");
    const quietBtnText = document.getElementById("quiet-count-text");
    const activeCount = document.getElementById("active-count");
    const quietList = document.getElementById("quiet-list");
    const lastSeenText = document.getElementById("last-seen-text");

    if (!moversList) return;

    try {
        const res = await fetch(`/api/watchlists/${currentWatchlistId}/delta?anchor=${currentAnchor}`);
        if (!res.ok) {
            setFreshness("Delayed");
            return;
        }
        const data = await res.json();

        setFreshness("Live");

        if (lastSeenText && data.lastSeenAt) {
            const dateObj = new Date(data.lastSeenAt);
            lastSeenText.innerText = currentAnchor === "SINCE_LAST_SEEN"
                ? `Diff computed relative to your last visit at ${dateObj.toLocaleTimeString()}`
                : `Diff computed relative to today's NSE market open baseline`;
        }

        // Render Active Movers
        moversList.innerHTML = "";
        const movers = data.activeMovers || [];
        if (activeCount) activeCount.innerText = movers.length;

        if (movers.length === 0) {
            moversList.innerHTML = `
                <div class="mover-row" style="color: var(--text-muted); font-size: 13px; text-align: center; justify-content: center; padding: 24px;">
                    Your watchlist is empty or has no active movers. Use the search bar above to search & add any NSE company!
                </div>`;
        } else {
            movers.forEach(mover => {
                const row = document.createElement("div");
                row.className = "mover-row";

                const formattedDelta = (mover.deltaPercent >= 0 ? "+" : "") + mover.deltaPercent.toFixed(2) + "%";
                const deltaClass = mover.deltaPercent >= 0 ? "delta-positive" : "delta-negative";

                const catalystHtml = mover.catalystBadgeText
                    ? `<span class="catalyst-tag">${mover.catalystBadgeText}</span>`
                    : "";

                const companySubtitle = mover.companyName && mover.companyName !== mover.symbol
                    ? `<span class="company-name">${mover.companyName}</span>`
                    : "";

                row.innerHTML = `
                    <div class="mover-left">
                        <span class="symbol">${mover.symbol}</span>
                        ${companySubtitle}
                        ${catalystHtml}
                    </div>
                    <div class="mover-right">
                        <span class="price">₹${mover.currentPrice.toFixed(2)}</span>
                        <span class="delta-badge ${deltaClass}">${formattedDelta}</span>
                    </div>
                `;
                moversList.appendChild(row);
            });
        }

        // Render Quiet Stocks
        const quietCount = data.quietCount || 0;
        if (quietBtnText) {
            quietBtnText.innerText = `${quietCount} quiet ${quietCount === 1 ? "stock" : "stocks"}`;
        }

        if (quietList) {
            quietList.innerHTML = "";
            const quietStocks = data.quietStocks || [];
            if (quietStocks.length === 0) {
                quietList.innerHTML = `<p class="quiet-note">No quiet stocks currently receding.</p>`;
            } else {
                quietStocks.forEach(qs => {
                    const qRow = document.createElement("div");
                    qRow.className = "quiet-row";
                    const formattedDelta = (qs.deltaPercent >= 0 ? "+" : "") + qs.deltaPercent.toFixed(2) + "%";
                    qRow.innerHTML = `
                        <span class="quiet-symbol">${qs.symbol} <span style="font-weight: normal; font-size: 11px; color: var(--text-muted);">(${qs.companyName || qs.symbol})</span></span>
                        <span class="quiet-price">₹${qs.currentPrice.toFixed(2)} (${formattedDelta})</span>
                    `;
                    quietList.appendChild(qRow);
                });
            }
        }

    } catch (e) {
        setFreshness("Reconnecting");
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
});
