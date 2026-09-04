package org.example.global.growwsmartwatchlist;

import org.example.global.growwsmartwatchlist.service.CatalystService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class CatalystServiceTaxonomyTest {

    @Autowired
    private CatalystService catalystService;

    @Test
    void testNormalizeCategoryDividend() {
        assertEquals("Dividend", catalystService.normalizeCategory("Declaration of Interim Dividend", null));
        assertEquals("Dividend", catalystService.normalizeCategory(null, "Special distribution of profits"));
    }

    @Test
    void testNormalizeCategoryEarnings() {
        assertEquals("Earnings", catalystService.normalizeCategory("Financial Results for Q3 FY26", null));
        assertEquals("Earnings", catalystService.normalizeCategory("Profit after tax report", null));
        assertEquals("Earnings", catalystService.normalizeCategory(null, "Quarterly earnings press release"));
    }

    @Test
    void testNormalizeCategoryBoardMeeting() {
        assertEquals("Board Meeting", catalystService.normalizeCategory("Notice of Board Meeting", null));
        assertEquals("Board Meeting", catalystService.normalizeCategory(null, "Outcome of general meeting"));
    }

    @Test
    void testNormalizeCategoryMaPartnership() {
        assertEquals("M&A / Partnership", catalystService.normalizeCategory("Strategic Acquisition of Tech Firm", null));
        assertEquals("M&A / Partnership", catalystService.normalizeCategory("Joint Venture Agreement", null));
        assertEquals("M&A / Partnership", catalystService.normalizeCategory(null, "Merger and Demerger scheme update"));
    }

    @Test
    void testNormalizeCategoryContractWin() {
        assertEquals("Contract Win", catalystService.normalizeCategory("New Order Win worth Rs 500 Cr", null));
        assertEquals("Contract Win", catalystService.normalizeCategory("Award of Contract from Ministry", null));
    }

    @Test
    void testNormalizeCategoryManagement() {
        assertEquals("Management", catalystService.normalizeCategory("Appointment of Independent Director", null));
        assertEquals("Management", catalystService.normalizeCategory("Resignation of Executive Director", null));
    }

    @Test
    void testNormalizeCategoryCorporateUpdateFallback() {
        assertEquals("Corporate Update", catalystService.normalizeCategory("Loss of Share Certificate", "Miscellaneous notification"));
        assertEquals("Corporate Update", catalystService.normalizeCategory(null, null));
    }
}
