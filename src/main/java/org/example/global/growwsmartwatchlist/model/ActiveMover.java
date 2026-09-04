package org.example.global.growwsmartwatchlist.model;

public class ActiveMover {
    private String symbol;
    private String companyName;
    private double currentPrice;
    private double deltaPercent;
    private double compositeScore;
    private String primaryDriver; // e.g. "Volume Surge 3.2x", "52w High Breakout"
    private boolean catalystFlag;
    private String catalystBadgeText;

    public ActiveMover() {}

    public ActiveMover(String symbol, String companyName, double currentPrice, double deltaPercent,
                       double compositeScore, String primaryDriver, boolean catalystFlag, String catalystBadgeText) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.currentPrice = currentPrice;
        this.deltaPercent = deltaPercent;
        this.compositeScore = compositeScore;
        this.primaryDriver = primaryDriver;
        this.catalystFlag = catalystFlag;
        this.catalystBadgeText = catalystBadgeText;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getDeltaPercent() { return deltaPercent; }
    public void setDeltaPercent(double deltaPercent) { this.deltaPercent = deltaPercent; }

    public double getCompositeScore() { return compositeScore; }
    public void setCompositeScore(double compositeScore) { this.compositeScore = compositeScore; }

    public String getPrimaryDriver() { return primaryDriver; }
    public void setPrimaryDriver(String primaryDriver) { this.primaryDriver = primaryDriver; }

    public boolean isCatalystFlag() { return catalystFlag; }
    public void setCatalystFlag(boolean catalystFlag) { this.catalystFlag = catalystFlag; }

    public String getCatalystBadgeText() { return catalystBadgeText; }
    public void setCatalystBadgeText(String catalystBadgeText) { this.catalystBadgeText = catalystBadgeText; }
}
