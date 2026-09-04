package org.example.global.growwsmartwatchlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.global.growwsmartwatchlist.model.DeltaResponse;
import org.example.global.growwsmartwatchlist.model.Watchlist;
import org.example.global.growwsmartwatchlist.service.DeltaComputationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WatchlistCatchUpE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private DeltaComputationService deltaComputationService;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void testEndToEndWatchlistCatchUpFlowOverHttp() throws Exception {
        // Force open market clock for active mover detection (Wednesday 11:00 AM IST)
        Instant openMarketTime = Instant.parse("2026-09-02T05:30:00Z");
        deltaComputationService.setClock(Clock.fixed(openMarketTime, ZoneId.of("Asia/Kolkata")));

        String baseUrl = "http://localhost:" + port;

        // 1. Create a watchlist via real HTTP POST /api/watchlists
        String createJson = "{\"userId\":101, \"name\":\"E2E Catch-Up Watchlist\"}";
        HttpRequest createReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/watchlists"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createJson))
                .build();

        HttpResponse<String> createRes = httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, createRes.statusCode(), "POST /api/watchlists should return 200 OK");

        Watchlist createdWatchlist = objectMapper.readValue(createRes.body(), Watchlist.class);
        assertNotNull(createdWatchlist);
        Long watchlistId = createdWatchlist.getId();
        assertNotNull(watchlistId, "Created watchlist ID must not be null");

        // 2. Add stock RELIANCE via real HTTP POST /api/watchlists/{id}/stocks
        String addStockJson = "{\"symbol\":\"RELIANCE\"}";
        HttpRequest addStockReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/watchlists/" + watchlistId + "/stocks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(addStockJson))
                .build();

        HttpResponse<String> addStockRes = httpClient.send(addStockReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, addStockRes.statusCode(), "POST /api/watchlists/{id}/stocks should return 200 OK");
        assertTrue(addStockRes.body().contains("\"success\":true"), "Response must indicate successful stock addition");

        // 3. Simulate time gap / tick processing latency
        Thread.sleep(100);

        // 4. Fetch delta summary via real HTTP GET /api/watchlists/{id}/delta?anchor=SINCE_OPEN
        HttpRequest deltaReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/watchlists/" + watchlistId + "/delta?anchor=SINCE_OPEN"))
                .GET()
                .build();

        HttpResponse<String> deltaRes = httpClient.send(deltaReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, deltaRes.statusCode(), "GET /api/watchlists/{id}/delta should return 200 OK");

        DeltaResponse body = objectMapper.readValue(deltaRes.body(), DeltaResponse.class);
        assertNotNull(body, "DeltaResponse body should not be null");
        assertEquals(watchlistId, body.getWatchlistId());

        // 5. Assert stock shows as an active mover in the HTTP response
        assertNotNull(body.getActiveMovers(), "Active movers list should not be null");
        assertFalse(body.getActiveMovers().isEmpty(), "Active movers list should contain movers");

        boolean containsReliance = body.getActiveMovers().stream()
                .anyMatch(m -> "RELIANCE".equalsIgnoreCase(m.getSymbol()));
        assertTrue(containsReliance, "Stock RELIANCE should be present in activeMovers list over HTTP");

        // 6. Assert synthesis summary is populated
        assertNotNull(body.getSynthesisSummary(), "Synthesis summary should not be null");
        assertFalse(body.getSynthesisSummary().isBlank(), "Synthesis summary should not be empty");
    }
}
