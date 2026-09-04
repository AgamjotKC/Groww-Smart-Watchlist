# ADR 0003: Hybrid In-Memory / Relational Storage Split

## Context
Watchlists and user profiles require ACID persistence, whereas tick streams and rolling metrics require high-throughput low-latency reads.

## Decision
Use relational DB (H2 in dev, PostgreSQL in prod) for Users, Watchlists, Watchlist Stocks, and Baskets. Use in-memory data structures (ConcurrentHashMap / Redis) for tick sliding windows and composite score caches.

## Consequences
- Clean isolation between relational entity domain and high-velocity tick metrics.
- Simplifies hackathon deployment without complex multi-database infrastructure dependencies.
