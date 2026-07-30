package com.nova.portfolio.service.impl;

import com.nova.portfolio.ai.PortfolioContextFormatter;
import com.nova.portfolio.ai.client.LlmClient;
import com.nova.portfolio.dto.AiResponseSource;
import com.nova.portfolio.dto.AssetAllocationSliceResponse;
import com.nova.portfolio.dto.InvestmentAdviceResponse;
import com.nova.portfolio.dto.PortfolioHoldingSummaryResponse;
import com.nova.portfolio.dto.PortfolioSummaryResponse;
import com.nova.portfolio.exception.AiServiceException;
import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.service.InvestmentAdviceService;
import com.nova.portfolio.service.PortfolioAnalyticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvestmentAdviceServiceImpl implements InvestmentAdviceService {

    private static final String DISCLAIMER =
        "This is general, automatically generated information, not personalized financial advice. "
            + "Consider consulting a licensed financial advisor before making investment decisions.";

    // Illustrative thresholds for the deterministic risk signals (not tied to any market data source).
    private static final BigDecimal CONCENTRATION_THRESHOLD_PERCENT = new BigDecimal("50");
    private static final BigDecimal CASH_THRESHOLD_PERCENT = new BigDecimal("30");
    private static final BigDecimal LOSS_THRESHOLD_PERCENT = new BigDecimal("-20");

    private final PortfolioAnalyticsService portfolioAnalyticsService;
    private final LlmClient llmClient;

    public InvestmentAdviceServiceImpl(PortfolioAnalyticsService portfolioAnalyticsService, LlmClient llmClient) {
        this.portfolioAnalyticsService = portfolioAnalyticsService;
        this.llmClient = llmClient;
    }

    @Override
    public InvestmentAdviceResponse generateAdvice(Long portfolioId) {
        PortfolioSummaryResponse summary = portfolioAnalyticsService.getPortfolioSummary(portfolioId);
        List<String> signals = deriveSignals(summary);

        String advice;
        AiResponseSource source;
        if (llmClient.isAvailable()) {
            try {
                advice = generateAiAdvice(summary, signals);
                source = AiResponseSource.AI;
            } catch (AiServiceException e) {
                advice = ruleBasedAdvice(signals);
                source = AiResponseSource.RULE_BASED;
            }
        } else {
            advice = ruleBasedAdvice(signals);
            source = AiResponseSource.RULE_BASED;
        }

        InvestmentAdviceResponse response = new InvestmentAdviceResponse();
        response.setPortfolioId(portfolioId);
        response.setAdvice(advice);
        response.setSignals(signals);
        response.setSource(source);
        response.setDisclaimer(DISCLAIMER);
        return response;
    }

    private String generateAiAdvice(PortfolioSummaryResponse summary, List<String> signals) {
        String systemPrompt = "You are an assistant that gives general, educational investment observations "
            + "about a single portfolio using ONLY the data provided below. Do not invent facts not present in "
            + "the data. Keep the response to 3-5 concise sentences. Do not claim to be a licensed financial advisor.";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append(PortfolioContextFormatter.formatSummary(summary));
        userPrompt.append("Detected signals:\n");
        if (signals.isEmpty()) {
            userPrompt.append("  - None of the checked risk signals (concentration, large drawdown, high idle cash) were triggered.\n");
        } else {
            for (String signal : signals) {
                userPrompt.append("  - ").append(signal).append('\n');
            }
        }
        userPrompt.append("Write general investment observations based on the above.");
        return llmClient.complete(systemPrompt, userPrompt.toString());
    }

    private String ruleBasedAdvice(List<String> signals) {
        if (signals.isEmpty()) {
            return "Your portfolio does not currently trigger any of the automated checks for concentration risk, "
                + "large unrealized losses, or high idle cash. This is not a full risk assessment.";
        }
        return String.join(" ", signals);
    }

    private List<String> deriveSignals(PortfolioSummaryResponse summary) {
        List<String> signals = new ArrayList<>();

        for (AssetAllocationSliceResponse slice : summary.getAllocation()) {
            if (slice.getPercent() == null) {
                continue;
            }
            if (slice.getPercent().compareTo(CONCENTRATION_THRESHOLD_PERCENT) > 0) {
                signals.add("High concentration in " + slice.getLabel() + " (" + slice.getPercent()
                    + "% of the portfolio) - consider diversifying.");
            }
            if (AssetType.CASH.name().equals(slice.getLabel())
                && slice.getPercent().compareTo(CASH_THRESHOLD_PERCENT) > 0) {
                signals.add("Cash makes up " + slice.getPercent()
                    + "% of the portfolio, which may be sitting idle instead of earning returns.");
            }
        }

        for (PortfolioHoldingSummaryResponse holding : summary.getHoldings()) {
            BigDecimal pnlPercent = unrealizedPnlPercent(holding);
            if (pnlPercent != null && pnlPercent.compareTo(LOSS_THRESHOLD_PERCENT) < 0) {
                signals.add(holding.getAssetLabel() + " is down " + pnlPercent.abs()
                    + "% from its average cost - consider reviewing this position.");
            }
        }

        return signals;
    }

    private BigDecimal unrealizedPnlPercent(PortfolioHoldingSummaryResponse holding) {
        if (holding.getUnrealizedPnl() == null || holding.getMarketValue() == null) {
            return null;
        }
        BigDecimal costBasis = holding.getMarketValue().subtract(holding.getUnrealizedPnl());
        if (costBasis.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return holding.getUnrealizedPnl()
            .divide(costBasis, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
}
