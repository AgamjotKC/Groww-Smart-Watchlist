package org.example.global.growwsmartwatchlist.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.example.global.growwsmartwatchlist.model.PrebuiltBasket;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BasketService {

    private final List<PrebuiltBasket> baskets = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/data/prebuilt_baskets.json")) {
            if (is != null) {
                List<PrebuiltBasket> loaded = objectMapper.readValue(is, new TypeReference<List<PrebuiltBasket>>() {});
                baskets.addAll(loaded);
            }
        } catch (Exception e) {
            // Keep empty list if unreadable
        }
    }

    public List<PrebuiltBasket> getAllBaskets() {
        return new ArrayList<>(baskets);
    }

    public Optional<PrebuiltBasket> getBasketByName(String name) {
        if (name == null) return Optional.empty();
        return baskets.stream()
                .filter(b -> b.getName().equalsIgnoreCase(name.trim()))
                .findFirst();
    }
}
