package com.nova.portfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CashAssetRequest {

    @NotBlank(message = "currency is required")
    @Size(max = 16, message = "currency must be <= 16 chars")
    private String currency;

    @NotNull(message = "exchangeRate is required")
    @DecimalMin(value = "0.000000", inclusive = false, message = "exchangeRate must be > 0")
    private BigDecimal exchangeRate;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }
}

