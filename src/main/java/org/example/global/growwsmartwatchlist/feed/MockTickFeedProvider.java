package org.example.global.growwsmartwatchlist.feed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.example.global.growwsmartwatchlist.model.StockTick;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class MockTickFeedProvider implements MarketDataFeed {

    private final List<StockTick> ticks = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/data/mock_ticks.json")) {
            if (is != null) {
                List<StockTick> loadedTicks = objectMapper.readValue(is, new TypeReference<List<StockTick>>() {});
                ticks.addAll(loadedTicks);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mock_ticks.json", e);
        }
    }

    @Override
    public List<StockTick> getLatestTicks() {
        return new ArrayList<>(ticks);
    }

    @Override
    public List<StockTick> getTicksForSymbol(String symbol) {
        if (symbol == null) return List.of();
        return ticks.stream()
                .filter(t -> t.getSymbol().equalsIgnoreCase(symbol))
                .toList();
    }

    @Override
    public StockTick getLatestTick(String symbol) {
        List<StockTick> symbolTicks = getTicksForSymbol(symbol);
        return symbolTicks.isEmpty() ? null : symbolTicks.get(symbolTicks.size() - 1);
    }
}
