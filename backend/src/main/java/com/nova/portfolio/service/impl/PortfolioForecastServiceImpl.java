package com.nova.portfolio.service.impl;

import com.nova.portfolio.dto.AssetAllocationSliceResponse;
import com.nova.portfolio.dto.ForecastPointResponse;
import com.nova.portfolio.dto.PortfolioForecastResponse;
import com.nova.portfolio.dto.PortfolioSummaryResponse;
import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.service.PortfolioAnalyticsService;
import com.nova.portfolio.service.PortfolioForecastService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioForecastServiceImpl implements PortfolioForecastService {

    private static final String DISCLAIMER =
        "Illustrative projection only, based on simplified long-run asset-class return/volatility "
            + "assumptions applied to the portfolio's current allocation. Not a guarantee of future "
            + "performance and not financial advice.";

    // Illustrative long-run annual return/volatility assumptions per asset class (not market data).
    private static final Map<AssetType, Double> ANNUAL_RETURN = Map.of(
        AssetType.STOCK, 0.07,
        AssetType.BOND, 0.03,
        AssetType.CASH, 0.015
    );
    private static final Map<AssetType, Double> ANNUAL_VOLATILITY = Map.of(
        AssetType.STOCK, 0.18,
        AssetType.BOND, 0.06,
        AssetType.CASH, 0.005
    );
    private static final double Z_SCORE_80_PCT = 1.2816;
    private static final int MIN_HORIZON_MONTHS = 1;
    private static final int MAX_HORIZON_MONTHS = 60;

    private final PortfolioAnalyticsService portfolioAnalyticsService;

    public PortfolioForecastServiceImpl(PortfolioAnalyticsService portfolioAnalyticsService) {
        this.portfolioAnalyticsService = portfolioAnalyticsService;
    }

    @Override
    public PortfolioForecastResponse forecast(Long portfolioId, int horizonMonths) {
        if (horizonMonths < MIN_HORIZON_MONTHS || horizonMonths > MAX_HORIZON_MONTHS) {
            throw new IllegalArgumentException(
                "horizonMonths must be between " + MIN_HORIZON_MONTHS + " and " + MAX_HORIZON_MONTHS);
        }

        PortfolioSummaryResponse summary = portfolioAnalyticsService.getPortfolioSummary(portfolioId);
        double currentValue = summary.getTotalValue() == null ? 0.0 : summary.getTotalValue().doubleValue();
        Map<AssetType, Double> weights = resolveWeights(summary.getAllocation(), currentValue);

        double mu = 0.0;
        double varianceSum = 0.0;
        for (Map.Entry<AssetType, Double> entry : weights.entrySet()) {
            double weight = entry.getValue();
            mu += weight * ANNUAL_RETURN.getOrDefault(entry.getKey(), 0.0);
            double vol = ANNUAL_VOLATILITY.getOrDefault(entry.getKey(), 0.0);
            varianceSum += weight * weight * vol * vol;
        }
        double sigma = Math.sqrt(varianceSum);

        List<ForecastPointResponse> points = new ArrayList<>();
        for (int month = 1; month <= horizonMonths; month++) {
            double t = month / 12.0;
            double central = currentValue * Math.pow(1 + mu, t);
            double drift = (mu - (sigma * sigma) / 2.0) * t;
            double spread = Z_SCORE_80_PCT * sigma * Math.sqrt(t);
            double upper = currentValue * Math.exp(drift + spread);
            double lower = currentValue * Math.exp(drift - spread);
            points.add(new ForecastPointResponse(month, money(central), money(lower), money(upper)));
        }

        PortfolioForecastResponse response = new PortfolioForecastResponse();
        response.setPortfolioId(portfolioId);
        response.setHorizonMonths(horizonMonths);
        response.setCurrentValue(money(currentValue));
        response.setAssumedAnnualReturnPercent(percent(mu));
        response.setAssumedAnnualVolatilityPercent(percent(sigma));
        response.setPoints(points);
        response.setDisclaimer(DISCLAIMER);
        return response;
    }

    private Map<AssetType, Double> resolveWeights(List<AssetAllocationSliceResponse> allocation, double totalValue) {
        Map<AssetType, Double> weights = new EnumMap<>(AssetType.class);
        if (totalValue <= 0) {
            return weights;
        }
        for (AssetAllocationSliceResponse slice : allocation) {
            AssetType assetType = resolveAssetType(slice.getLabel());
            if (assetType == null || slice.getValue() == null) {
                continue;
            }
            weights.put(assetType, slice.getValue().doubleValue() / totalValue);
        }
        return weights;
    }

    private AssetType resolveAssetType(String label) {
        if (label == null) {
            return null;
        }
        try {
            return AssetType.valueOf(label);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(double value) {
        return BigDecimal.valueOf(value * 100).setScale(2, RoundingMode.HALF_UP);
    }
}
