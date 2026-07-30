package com.nova.portfolio.dto;

import java.math.BigDecimal;

public class ForecastPointResponse {

    private int month;
    private BigDecimal projectedValue;
    private BigDecimal lowerBound;
    private BigDecimal upperBound;

    public ForecastPointResponse() {
    }

    public ForecastPointResponse(int month, BigDecimal projectedValue, BigDecimal lowerBound, BigDecimal upperBound) {
        this.month = month;
        this.projectedValue = projectedValue;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public BigDecimal getProjectedValue() {
        return projectedValue;
    }

    public void setProjectedValue(BigDecimal projectedValue) {
        this.projectedValue = projectedValue;
    }

    public BigDecimal getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(BigDecimal lowerBound) {
        this.lowerBound = lowerBound;
    }

    public BigDecimal getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(BigDecimal upperBound) {
        this.upperBound = upperBound;
    }
}
