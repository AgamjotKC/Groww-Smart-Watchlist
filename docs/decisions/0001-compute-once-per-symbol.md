# ADR 0001: Compute-Once-Per-Symbol Precomputed Snapshot Scoring

## Context
When a user requests a Catch-Up watchlist delta, calculating rolling Z-scores and volume ratios dynamically per watchlist item across all raw ticks would require $O(\text{Users} \times \text{Symbols})$ compute at query time.

## Decision
Compute and cache relevance scores at the symbol level continuously as ticks arrive. Store the latest precomputed score snapshot per symbol.

## Consequences
- Delta computation endpoint runs in $O(N \log N)$ time, performing filtering and sorting only over pre-scored symbol snapshots.
- Guarantees sub-50ms API response time for Catch-Up Card rendering.
