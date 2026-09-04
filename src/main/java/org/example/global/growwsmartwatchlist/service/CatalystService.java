package org.example.global.growwsmartwatchlist.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.example.global.growwsmartwatchlist.model.Catalyst;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CatalystService {

    private final List<Catalyst> catalysts = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/data/mock_filings.json")) {
            if (is != null) {
                List<Catalyst> loaded = objectMapper.readValue(is, new TypeReference<List<Catalyst>>() {});
                catalysts.addAll(loaded);
            }
        } catch (Exception e) {
            // Keep empty list if unreadable
        }
    }

    public boolean hasCatalyst(String symbol) {
        if (symbol == null) return false;
        return catalysts.stream().anyMatch(c -> c.getSymbol().equalsIgnoreCase(symbol));
    }

    public Optional<Catalyst> getLatestCatalyst(String symbol) {
        if (symbol == null) return Optional.empty();
        return catalysts.stream()
                .filter(c -> c.getSymbol().equalsIgnoreCase(symbol))
                .findFirst();
    }
}
