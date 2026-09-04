package org.example.global.growwsmartwatchlist.model;

public class ActiveMover {
    private String symbol;
    private String companyName;
    private double currentPrice;
    private double deltaPercent;
    private double compositeScore;
    private String primaryDriver;
    private boolean catalystFlag;
    private String catalystBadgeText;
    private String newsUrl;
    private double volumeSurgeRatio;
    private double volatilityZScore;
    private double week52High;
    private double week52Low;

    public ActiveMover() {}

    public ActiveMover(String symbol, String companyName, double currentPrice, double deltaPercent,
                       double compositeScore, String primaryDriver, boolean catalystFlag, String catalystBadgeText,
                       String newsUrl, double volumeSurgeRatio, double volatilityZScore,
                       double week52High, double week52Low) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.currentPrice = currentPrice;
        this.deltaPercent = deltaPercent;
        this.compositeScore = compositeScore;
        this.primaryDriver = primaryDriver;
        this.catalystFlag = catalystFlag;
        this.catalystBadgeText = catalystBadgeText;
        this.newsUrl = newsUrl;
        this.volumeSurgeRatio = volumeSurgeRatio;
        this.volatilityZScore = volatilityZScore;
        this.week52High = week52High;
        this.week52Low = week52Low;
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

    public String getNewsUrl() { return newsUrl; }
    public void setNewsUrl(String newsUrl) { this.newsUrl = newsUrl; }

    public double getVolumeSurgeRatio() { return volumeSurgeRatio; }
    public void setVolumeSurgeRatio(double volumeSurgeRatio) { this.volumeSurgeRatio = volumeSurgeRatio; }

    public double getVolatilityZScore() { return volatilityZScore; }
    public void setVolatilityZScore(double volatilityZScore) { this.volatilityZScore = volatilityZScore; }

    public double getWeek52High() { return week52High; }
    public void setWeek52High(double week52High) { this.week52High = week52High; }

    public double getWeek52Low() { return week52Low; }
    public void setWeek52Low(double week52Low) { this.week52Low = week52Low; }
}
