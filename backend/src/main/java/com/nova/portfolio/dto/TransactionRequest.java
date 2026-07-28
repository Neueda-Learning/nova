package com.nova.portfolio.dto;

import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionRequest {

    @NotNull(message = "portfolioId is required")
    private Long portfolioId;

    @NotNull(message = "assetType is required")
    private AssetType assetType;

    @NotNull(message = "assetId is required")
    private Long assetId;

    @NotNull(message = "transactionType is required")
    private TransactionType transactionType;

    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0.0001", message = "quantity must be >= 0.0001")
    private BigDecimal quantity;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.0000", inclusive = false, message = "price must be > 0")
    private BigDecimal price;

    @NotNull(message = "transactionDate is required")
    @PastOrPresent(message = "transactionDate cannot be in the future")
    private LocalDate transactionDate;

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

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }
}
