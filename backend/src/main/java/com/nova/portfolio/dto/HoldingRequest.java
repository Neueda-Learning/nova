package com.nova.portfolio.dto;

import com.nova.portfolio.model.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class HoldingRequest {

    @NotNull(message = "portfolioId is required")
    private Long portfolioId;

    @NotNull(message = "assetType is required")
    private AssetType assetType;

    @NotNull(message = "assetId is required")
    private Long assetId;

    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0.0001", message = "quantity must be >= 0.0001")
    private BigDecimal quantity;

    public Long getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
