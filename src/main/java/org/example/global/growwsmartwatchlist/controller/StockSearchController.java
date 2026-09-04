package org.example.global.growwsmartwatchlist.controller;

import org.example.global.growwsmartwatchlist.feed.NseMarketDataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping({"/stocks", "/api/stocks"})
public class StockSearchController {

    @Autowired
    private NseMarketDataProvider nseMarketDataProvider;

    public static class StockSearchResult {
        private String symbol;
        private String companyName;

        public StockSearchResult() {}

        public StockSearchResult(String symbol, String companyName) {
            this.symbol = symbol;
            this.companyName = companyName;
        }

        public String getSymbol() { return symbol; }
        public String getCompanyName() { return companyName; }
    }

    private static final List<StockSearchResult> POPULAR_NSE_STOCKS = List.of(
            new StockSearchResult("RELIANCE", "Reliance Industries Ltd"),
            new StockSearchResult("TCS", "Tata Consultancy Services Ltd"),
            new StockSearchResult("INFY", "Infosys Ltd"),
            new StockSearchResult("HDFCBANK", "HDFC Bank Ltd"),
            new StockSearchResult("ICICIBANK", "ICICI Bank Ltd"),
            new StockSearchResult("SBIN", "State Bank of India"),
            new StockSearchResult("BHARTIARTL", "Bharti Airtel Ltd"),
            new StockSearchResult("ITC", "ITC Ltd"),
            new StockSearchResult("KOTAKBANK", "Kotak Mahindra Bank Ltd"),
            new StockSearchResult("LT", "Larsen & Toubro Ltd"),
            new StockSearchResult("AXISBANK", "Axis Bank Ltd"),
            new StockSearchResult("HCLTECH", "HCL Technologies Ltd"),
            new StockSearchResult("ASIANPAINT", "Asian Paints Ltd"),
            new StockSearchResult("MARUTI", "Maruti Suzuki India Ltd"),
            new StockSearchResult("SUNPHARMA", "Sun Pharmaceutical Industries Ltd"),
            new StockSearchResult("TITAN", "Titan Company Ltd"),
            new StockSearchResult("BAJFINANCE", "Bajaj Finance Ltd"),
            new StockSearchResult("TATAMOTORS", "Tata Motors Ltd"),
            new StockSearchResult("WIPRO", "Wipro Ltd"),
            new StockSearchResult("ULTRACEMCO", "UltraTech Cement Ltd"),
            new StockSearchResult("POWERGRID", "Power Grid Corp of India Ltd"),
            new StockSearchResult("NTPC", "NTPC Ltd"),
            new StockSearchResult("TATASTEEL", "Tata Steel Ltd"),
            new StockSearchResult("JSWSTEEL", "JSW Steel Ltd"),
            new StockSearchResult("M&M", "Mahindra & Mahindra Ltd"),
            new StockSearchResult("ADANIENT", "Adani Enterprises Ltd"),
            new StockSearchResult("ADANIPORTS", "Adani Ports & SEZ Ltd"),
            new StockSearchResult("COALINDIA", "Coal India Ltd"),
            new StockSearchResult("LTIM", "LTIMindtree Ltd"),
            new StockSearchResult("TECHM", "Tech Mahindra Ltd"),
            new StockSearchResult("SUZLON", "Suzlon Energy Ltd"),
            new StockSearchResult("ZOMATO", "Zomato Ltd"),
            new StockSearchResult("TATAPOWER", "Tata Power Company Ltd"),
            new StockSearchResult("PAYTM", "One97 Communications / Paytm"),
            new StockSearchResult("IRCTC", "Indian Railway Catering & Tourism"),
            new StockSearchResult("NYKAA", "FSN E-Commerce / Nykaa"),
            new StockSearchResult("BEL", "Bharat Electronics Ltd"),
            new StockSearchResult("HAL", "Hindustan Aeronautics Ltd"),
            new StockSearchResult("POLICYBZR", "PB Fintech / Policybazaar"),
            new StockSearchResult("DELHIVERY", "Delhivery Ltd"),
            new StockSearchResult("PERSISTENT", "Persistent Systems Ltd"),
            new StockSearchResult("COFORGE", "Coforge Ltd"),
            new StockSearchResult("MPHASIS", "Mphasis Ltd"),
            new StockSearchResult("TATAELXSI", "Tata Elxsi Ltd"),
            new StockSearchResult("TRENT", "Trent Ltd"),
            new StockSearchResult("DIXON", "Dixon Technologies Ltd"),
            new StockSearchResult("POLYCAB", "Polycab India Ltd")
    );

    @GetMapping("/search")
    public ResponseEntity<List<StockSearchResult>> searchStocks(@RequestParam(required = false, defaultValue = "") String q) {
        String query = q.trim().toLowerCase();
        if (query.isEmpty()) {
            return ResponseEntity.ok(POPULAR_NSE_STOCKS.subList(0, 10));
        }

        List<StockSearchResult> results = new ArrayList<>();
        for (StockSearchResult stock : POPULAR_NSE_STOCKS) {
            if (stock.getSymbol().toLowerCase().contains(query) || stock.getCompanyName().toLowerCase().contains(query)) {
                results.add(stock);
            }
        }

        // If query is custom and not in popular list, add custom symbol candidate
        if (results.isEmpty() && query.length() >= 2) {
            String upper = query.toUpperCase();
            results.add(new StockSearchResult(upper, upper + " (NSE Equities)"));
        }

        return ResponseEntity.ok(results);
    }
}
