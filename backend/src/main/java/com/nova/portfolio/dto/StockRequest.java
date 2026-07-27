package com.nova.portfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class StockRequest {

    @NotBlank(message = "symbol is required")
    @Size(max = 32, message = "symbol must be <= 32 chars")
    private String symbol;

    @NotBlank(message = "name is required")
    @Size(max = 128, message = "name must be <= 128 chars")
    private String name;

    @NotBlank(message = "sector is required")
    @Size(max = 64, message = "sector must be <= 64 chars")
    private String sector;

    @NotBlank(message = "exchange is required")
    @Size(max = 64, message = "exchange must be <= 64 chars")
    private String exchange;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.0000", inclusive = false, message = "price must be > 0")
    private BigDecimal price;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}

