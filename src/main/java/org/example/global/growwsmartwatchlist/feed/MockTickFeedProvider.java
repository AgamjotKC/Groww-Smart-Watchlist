package org.example.global.growwsmartwatchlist.feed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.example.global.growwsmartwatchlist.model.StockTick;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MockTickFeedProvider implements MarketDataFeed {

    private final List<StockTick> initialScenarioTicks = new ArrayList<>();
    private final List<StockTick> emittedTicks = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger cursor = new AtomicInteger(0);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/data/mock_ticks.json")) {
            if (is != null) {
                List<StockTick> loadedTicks = objectMapper.readValue(is, new TypeReference<List<StockTick>>() {});
                initialScenarioTicks.addAll(loadedTicks);
                if (!initialScenarioTicks.isEmpty()) {
                    emittedTicks.add(initialScenarioTicks.get(0));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mock_ticks.json", e);
        }
    }

    @Scheduled(fixedRate = 2000)
    public void advanceTickStream() {
        if (initialScenarioTicks.isEmpty()) return;
        int nextIndex = cursor.incrementAndGet() % initialScenarioTicks.size();
        StockTick source = initialScenarioTicks.get(nextIndex);
        StockTick liveTick = new StockTick(source.getSymbol(), source.getPrice(), source.getVolume(), Instant.now());
        emittedTicks.add(liveTick);
    }

    @Override
    public List<StockTick> getLatestTicks() {
        synchronized (emittedTicks) {
            return new ArrayList<>(emittedTicks);
        }
    }

    @Override
    public List<StockTick> getTicksForSymbol(String symbol) {
        if (symbol == null) return List.of();
        synchronized (emittedTicks) {
            return emittedTicks.stream()
                    .filter(t -> t.getSymbol().equalsIgnoreCase(symbol))
                    .toList();
        }
    }

    @Override
    public StockTick getLatestTick(String symbol) {
        List<StockTick> symbolTicks = getTicksForSymbol(symbol);
        return symbolTicks.isEmpty() ? null : symbolTicks.get(symbolTicks.size() - 1);
    }
}
