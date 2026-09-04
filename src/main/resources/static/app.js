let currentAnchor = "SINCE_LAST_SEEN";

function setFreshness(status) {
    const badge = document.getElementById("freshness-badge");
    if (!badge) return;

    badge.className = `freshness-badge ${status.toLowerCase()}`;
    badge.innerText = status;
}

async function loadCatchUpCard() {
    const moversList = document.getElementById("movers-list");
    const quietBtn = document.getElementById("quiet-toggle-btn");
    if (!moversList) return;

    try {
        const res = await fetch(`/api/watchlists/1/delta?anchor=${currentAnchor}`);
        if (!res.ok) {
            setFreshness("Delayed");
            return;
        }
        const data = await res.json();

        setFreshness("Live");

        moversList.innerHTML = "";
        data.activeMovers.forEach(mover => {
            const row = document.createElement("div");
            row.className = "mover-row";

            const formattedDelta = (mover.deltaPercent >= 0 ? "+" : "") + mover.deltaPercent.toFixed(2) + "%";
            const deltaClass = mover.deltaPercent >= 0 ? "delta-positive" : "delta-negative";

            row.innerHTML = `
                <span class="symbol">${mover.symbol}</span>
                <span class="price">₹${mover.currentPrice.toFixed(2)}</span>
                <span class="delta ${deltaClass}">${formattedDelta}</span>
                <span class="badge">${mover.catalystBadgeText || ""}</span>
            `;
            moversList.appendChild(row);
        });

        if (quietBtn) {
            quietBtn.innerText = `${data.quietCount} quiet ${data.quietCount === 1 ? "stock" : "stocks"}`;
        }
    } catch (e) {
        console.error("Failed to load delta", e);
        setFreshness("Reconnecting");
    }
}

document.addEventListener("DOMContentLoaded", () => {
    loadCatchUpCard();

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

    const quietBtn = document.getElementById("quiet-toggle-btn");
    const quietContent = document.getElementById("quiet-content");
    if (quietBtn && quietContent) {
        quietBtn.addEventListener("click", () => {
            quietContent.classList.toggle("hidden");
        });
    }
});
