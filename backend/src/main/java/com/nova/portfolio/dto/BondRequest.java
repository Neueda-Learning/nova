package com.nova.portfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BondRequest {

    @NotBlank(message = "name is required")
    @Size(max = 128, message = "name must be <= 128 chars")
    private String name;

    @NotBlank(message = "bondType is required")
    @Size(max = 64, message = "bondType must be <= 64 chars")
    private String bondType;

    @NotBlank(message = "issuer is required")
    @Size(max = 128, message = "issuer must be <= 128 chars")
    private String issuer;

    @NotNull(message = "interestRate is required")
    @DecimalMin(value = "0.0000", inclusive = false, message = "interestRate must be > 0")
    private BigDecimal interestRate;

    @NotNull(message = "maturityDate is required")
    @Future(message = "maturityDate must be in the future")
    private LocalDate maturityDate;

    @NotNull(message = "currentPrice is required")
    @DecimalMin(value = "0.0000", inclusive = false, message = "currentPrice must be > 0")
    private BigDecimal currentPrice;

    @NotBlank(message = "riskLevel is required")
    @Size(max = 32, message = "riskLevel must be <= 32 chars")
    private String riskLevel;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBondType() {
        return bondType;
    }

    public void setBondType(String bondType) {
        this.bondType = bondType;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}

