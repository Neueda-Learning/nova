package com.nova.portfolio.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PortfolioForecastResponse {

    private Long portfolioId;
    private int horizonMonths;
    private BigDecimal currentValue;
    private BigDecimal assumedAnnualReturnPercent;
    private BigDecimal assumedAnnualVolatilityPercent;
    private List<ForecastPointResponse> points = new ArrayList<>();
    private String disclaimer;

    public Long getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }

    public int getHorizonMonths() {
        return horizonMonths;
    }

    public void setHorizonMonths(int horizonMonths) {
        this.horizonMonths = horizonMonths;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getAssumedAnnualReturnPercent() {
        return assumedAnnualReturnPercent;
    }

    public void setAssumedAnnualReturnPercent(BigDecimal assumedAnnualReturnPercent) {
        this.assumedAnnualReturnPercent = assumedAnnualReturnPercent;
    }

    public BigDecimal getAssumedAnnualVolatilityPercent() {
        return assumedAnnualVolatilityPercent;
    }

    public void setAssumedAnnualVolatilityPercent(BigDecimal assumedAnnualVolatilityPercent) {
        this.assumedAnnualVolatilityPercent = assumedAnnualVolatilityPercent;
    }

    public List<ForecastPointResponse> getPoints() {
        return points;
    }

    public void setPoints(List<ForecastPointResponse> points) {
        this.points = points;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
