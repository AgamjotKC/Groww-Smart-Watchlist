async function loadCatchUpCard() {
    const moversList = document.getElementById("movers-list");
    if (!moversList) return;

    try {
        const res = await fetch("/api/watchlists/1/delta?anchor=SINCE_LAST_SEEN");
        const data = await res.json();

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
    } catch (e) {
        console.error("Failed to load delta", e);
    }
}

document.addEventListener("DOMContentLoaded", loadCatchUpCard);
