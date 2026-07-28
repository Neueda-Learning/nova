package com.nova.portfolio.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PortfolioDashboardResponse {

    private List<PortfolioSummaryResponse> portfolios = new ArrayList<>();
    private List<AssetAllocationSliceResponse> allocation = new ArrayList<>();
    private BigDecimal globalTotal;

    public List<PortfolioSummaryResponse> getPortfolios() {
        return portfolios;
    }

    public void setPortfolios(List<PortfolioSummaryResponse> portfolios) {
        this.portfolios = portfolios;
    }

    public List<AssetAllocationSliceResponse> getAllocation() {
        return allocation;
    }

    public void setAllocation(List<AssetAllocationSliceResponse> allocation) {
        this.allocation = allocation;
    }

    public BigDecimal getGlobalTotal() {
        return globalTotal;
    }

    public void setGlobalTotal(BigDecimal globalTotal) {
        this.globalTotal = globalTotal;
    }
}

