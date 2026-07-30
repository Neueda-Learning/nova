package com.nova.portfolio.dto;

import java.math.BigDecimal;

public class ActivitySummaryResponse {

    private Long portfolioId;
    private int periodDays;
    private int buyCount;
    private int sellCount;
    private BigDecimal netCashFlow;
    private String mostActiveAsset;
    private String summary;
    private AiResponseSource source;

    public Long getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }

    public int getPeriodDays() {
        return periodDays;
    }

    public void setPeriodDays(int periodDays) {
        this.periodDays = periodDays;
    }

    public int getBuyCount() {
        return buyCount;
    }

    public void setBuyCount(int buyCount) {
        this.buyCount = buyCount;
    }

    public int getSellCount() {
        return sellCount;
    }

    public void setSellCount(int sellCount) {
        this.sellCount = sellCount;
    }

    public BigDecimal getNetCashFlow() {
        return netCashFlow;
    }

    public void setNetCashFlow(BigDecimal netCashFlow) {
        this.netCashFlow = netCashFlow;
    }

    public String getMostActiveAsset() {
        return mostActiveAsset;
    }

    public void setMostActiveAsset(String mostActiveAsset) {
        this.mostActiveAsset = mostActiveAsset;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public AiResponseSource getSource() {
        return source;
    }

    public void setSource(AiResponseSource source) {
        this.source = source;
    }
}
