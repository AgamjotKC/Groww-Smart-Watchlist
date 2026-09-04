package org.example.global.growwsmartwatchlist.model;

public class SymbolMetrics {
    private String symbol;
    private double currentPrice;
    private double priceChangePercent;
    private double volatilityZScore;
    private double volumeSurgeRatio;
    private double levelProximityScore;
    private boolean hasCatalyst;
    private String catalystText;
    private double compositeRelevanceScore;

    public SymbolMetrics() {}

    public SymbolMetrics(String symbol, double currentPrice, double priceChangePercent, double volatilityZScore,
                         double volumeSurgeRatio, double levelProximityScore, boolean hasCatalyst,
                         String catalystText, double compositeRelevanceScore) {
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.priceChangePercent = priceChangePercent;
        this.volatilityZScore = volatilityZScore;
        this.volumeSurgeRatio = volumeSurgeRatio;
        this.levelProximityScore = levelProximityScore;
        this.hasCatalyst = hasCatalyst;
        this.catalystText = catalystText;
        this.compositeRelevanceScore = compositeRelevanceScore;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getPriceChangePercent() { return priceChangePercent; }
    public void setPriceChangePercent(double priceChangePercent) { this.priceChangePercent = priceChangePercent; }

    public double getVolatilityZScore() { return volatilityZScore; }
    public void setVolatilityZScore(double volatilityZScore) { this.volatilityZScore = volatilityZScore; }

    public double getVolumeSurgeRatio() { return volumeSurgeRatio; }
    public void setVolumeSurgeRatio(double volumeSurgeRatio) { this.volumeSurgeRatio = volumeSurgeRatio; }

    public double getLevelProximityScore() { return levelProximityScore; }
    public void setLevelProximityScore(double levelProximityScore) { this.levelProximityScore = levelProximityScore; }

    public boolean isHasCatalyst() { return hasCatalyst; }
    public void setHasCatalyst(boolean hasCatalyst) { this.hasCatalyst = hasCatalyst; }

    public String getCatalystText() { return catalystText; }
    public void setCatalystText(String catalystText) { this.catalystText = catalystText; }

    public double getCompositeRelevanceScore() { return compositeRelevanceScore; }
    public void setCompositeRelevanceScore(double compositeRelevanceScore) { this.compositeRelevanceScore = compositeRelevanceScore; }
}
