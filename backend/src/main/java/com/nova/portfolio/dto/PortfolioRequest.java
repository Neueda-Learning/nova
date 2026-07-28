package com.nova.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PortfolioRequest {

    @NotBlank(message = "portfolioName is required")
    @Size(max = 128, message = "portfolioName must be <= 128 chars")
    private String portfolioName;

    @Size(max = 255, message = "description must be <= 255 chars")
    private String description;

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
}

