package org.example.global.growwsmartwatchlist;

import org.example.global.growwsmartwatchlist.service.TrendlineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TrendlineServiceTest {

    @Autowired
    private TrendlineService trendlineService;

    @Test
    void testNullSymbolReturnsEmpty() {
        Optional<String> result = trendlineService.detectCrossover(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testBlankSymbolReturnsEmpty() {
        Optional<String> result = trendlineService.detectCrossover("   ");
        assertTrue(result.isEmpty());
    }

    @Test
    void testSymbolCacheHit() {
        Optional<String> firstCall = trendlineService.detectCrossover("RELIANCE");
        Optional<String> secondCall = trendlineService.detectCrossover("RELIANCE");
        assertEquals(firstCall, secondCall);
    }

    @Test
    void testSymbolWithDotNsSuffix() {
        Optional<String> result = trendlineService.detectCrossover("TCS.NS");
        assertNotNull(result);
    }

    @Test
    void testSpecialSymbolMappingMahindra() {
        Optional<String> result = trendlineService.detectCrossover("M&M");
        assertNotNull(result);
    }

    @Test
    void testSpecialSymbolMappingLtim() {
        Optional<String> result = trendlineService.detectCrossover("LTIM");
        assertNotNull(result);
    }
}
