# ADR 0002: Server-Side `last_seen_at` Persistence per User/Watchlist

## Context
Catch-Up requires knowing the user's previous reference timestamp (`last_seen_at`) to calculate price deltas and active movers.

## Decision
Persist `last_seen_at` in the database per user per watchlist on every explicitly recorded catch-up view session.

## Consequences
- Prevents desynchronization across devices or cleared browser client storage.
- Enables consistent anchor toggling ("Since Last Seen" vs. "Since Open").
