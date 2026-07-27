package com.nova.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PortfolioRequest {

    @NotBlank(message = "portfolioName is required")
    @Size(max = 128, message = "portfolioName must be <= 128 chars")
    private String portfolioName;

    public String getPortfolioName() {
        return portfolioName;
    }

    public void setPortfolioName(String portfolioName) {
        this.portfolioName = portfolioName;
    }
}

