package com.nova.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PortfolioSummaryResponse {

    private Long id;
    private String portfolioName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal totalValue;
    private BigDecimal totalPnl;
    private boolean hasPnlData;
    private int holdingCount;
    private List<AssetAllocationSliceResponse> allocation = new ArrayList<>();
    private List<PortfolioHoldingSummaryResponse> holdings = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPortfolioName() {
        return portfolioName;
    }

    public void setPortfolioName(String portfolioName) {
        this.portfolioName = portfolioName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getTotalPnl() {
        return totalPnl;
    }

    public void setTotalPnl(BigDecimal totalPnl) {
        this.totalPnl = totalPnl;
    }

    public boolean isHasPnlData() {
        return hasPnlData;
    }

    public void setHasPnlData(boolean hasPnlData) {
        this.hasPnlData = hasPnlData;
    }

    public int getHoldingCount() {
        return holdingCount;
    }

    public void setHoldingCount(int holdingCount) {
        this.holdingCount = holdingCount;
    }

    public List<AssetAllocationSliceResponse> getAllocation() {
        return allocation;
    }

    public void setAllocation(List<AssetAllocationSliceResponse> allocation) {
        this.allocation = allocation;
    }

    public List<PortfolioHoldingSummaryResponse> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<PortfolioHoldingSummaryResponse> holdings) {
        this.holdings = holdings;
    }
}

