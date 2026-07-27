package com.nova.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public class HoldingRequest {

    @NotBlank(message = "stockTicker is required")
    @Pattern(regexp = "^[A-Za-z.]{1,16}$", message = "stockTicker must be 1-16 letters or dot")
    private String stockTicker;

    @Positive(message = "volume must be > 0")
    private Integer volume;

    public String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }
}
