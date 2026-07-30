package com.nova.portfolio.service.impl;

import com.nova.portfolio.ai.PortfolioContextFormatter;
import com.nova.portfolio.ai.client.LlmClient;
import com.nova.portfolio.dto.ActivitySummaryResponse;
import com.nova.portfolio.dto.AiResponseSource;
import com.nova.portfolio.dto.PortfolioSummaryResponse;
import com.nova.portfolio.dto.TransactionResponse;
import com.nova.portfolio.exception.AiServiceException;
import com.nova.portfolio.mapper.TransactionMapper;
import com.nova.portfolio.model.Transaction;
import com.nova.portfolio.model.TransactionType;
import com.nova.portfolio.repository.TransactionRepository;
import com.nova.portfolio.service.PortfolioActivitySummaryService;
import com.nova.portfolio.service.PortfolioAnalyticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PortfolioActivitySummaryServiceImpl implements PortfolioActivitySummaryService {

    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 365;

    private final PortfolioAnalyticsService portfolioAnalyticsService;
    private final TransactionRepository transactionRepository;
    private final LlmClient llmClient;

    public PortfolioActivitySummaryServiceImpl(
        PortfolioAnalyticsService portfolioAnalyticsService,
        TransactionRepository transactionRepository,
        LlmClient llmClient
    ) {
        this.portfolioAnalyticsService = portfolioAnalyticsService;
        this.transactionRepository = transactionRepository;
        this.llmClient = llmClient;
    }

    @Override
    public ActivitySummaryResponse summarize(Long portfolioId, int days) {
        if (days < MIN_DAYS || days > MAX_DAYS) {
            throw new IllegalArgumentException("days must be between " + MIN_DAYS + " and " + MAX_DAYS);
        }

        PortfolioSummaryResponse portfolioSummary = portfolioAnalyticsService.getPortfolioSummary(portfolioId);
        LocalDate since = LocalDate.now().minusDays(days);
        List<Transaction> transactions = transactionRepository
            .findByPortfolioIdAndTransactionDateGreaterThanEqualOrderByTransactionDateDescIdDesc(portfolioId, since);

        ActivitySummaryResponse response = new ActivitySummaryResponse();
        response.setPortfolioId(portfolioId);
        response.setPeriodDays(days);

        if (transactions.isEmpty()) {
            response.setBuyCount(0);
            response.setSellCount(0);
            response.setNetCashFlow(BigDecimal.ZERO);
            response.setMostActiveAsset(null);
            response.setSummary("No transactions were recorded in the last " + days + " day(s).");
            response.setSource(AiResponseSource.RULE_BASED);
            return response;
        }

        int buyCount = 0;
        int sellCount = 0;
        BigDecimal netCashFlow = BigDecimal.ZERO;
        Map<String, Long> activityCounts = new HashMap<>();

        for (Transaction transaction : transactions) {
            BigDecimal notional = transaction.getQuantity().multiply(transaction.getPrice());
            if (transaction.getTransactionType() == TransactionType.BUY) {
                buyCount++;
                netCashFlow = netCashFlow.subtract(notional);
            } else {
                sellCount++;
                netCashFlow = netCashFlow.add(notional);
            }
            String assetKey = transaction.getAssetType() + " #" + transaction.getAssetId();
            activityCounts.merge(assetKey, 1L, Long::sum);
        }

        String mostActiveAsset = activityCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);

        response.setBuyCount(buyCount);
        response.setSellCount(sellCount);
        response.setNetCashFlow(netCashFlow);
        response.setMostActiveAsset(mostActiveAsset);

        String summaryText;
        AiResponseSource source;
        if (llmClient.isAvailable()) {
            try {
                summaryText = generateAiSummary(portfolioSummary, transactions, days, buyCount, sellCount, netCashFlow, mostActiveAsset);
                source = AiResponseSource.AI;
            } catch (AiServiceException e) {
                summaryText = ruleBasedSummary(days, buyCount, sellCount, netCashFlow, mostActiveAsset);
                source = AiResponseSource.RULE_BASED;
            }
        } else {
            summaryText = ruleBasedSummary(days, buyCount, sellCount, netCashFlow, mostActiveAsset);
            source = AiResponseSource.RULE_BASED;
        }
        response.setSummary(summaryText);
        response.setSource(source);
        return response;
    }

    private String generateAiSummary(
        PortfolioSummaryResponse portfolioSummary,
        List<Transaction> transactions,
        int days,
        int buyCount,
        int sellCount,
        BigDecimal netCashFlow,
        String mostActiveAsset
    ) {
        String systemPrompt = "You are an assistant that writes a short, factual, plain-English narrative summary "
            + "of a portfolio's recent trading activity using ONLY the data provided below. Keep it to 3-5 sentences.";

        List<TransactionResponse> transactionResponses = transactions.stream()
            .map(TransactionMapper::toResponse)
            .collect(Collectors.toList());

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append(PortfolioContextFormatter.formatSummary(portfolioSummary));
        userPrompt.append(PortfolioContextFormatter.formatTransactions(transactionResponses));
        userPrompt.append("Period: last ").append(days).append(" day(s)\n");
        userPrompt.append("Buys: ").append(buyCount).append(", Sells: ").append(sellCount).append('\n');
        userPrompt.append("Net cash flow: ").append(netCashFlow).append('\n');
        userPrompt.append("Most active asset: ").append(mostActiveAsset).append('\n');
        userPrompt.append("Write a short narrative summary of this activity.");
        return llmClient.complete(systemPrompt, userPrompt.toString());
    }

    private String ruleBasedSummary(int days, int buyCount, int sellCount, BigDecimal netCashFlow, String mostActiveAsset) {
        String flowDescription = netCashFlow.compareTo(BigDecimal.ZERO) >= 0
            ? "a net cash inflow of " + netCashFlow.abs()
            : "a net cash outflow of " + netCashFlow.abs();
        return "Over the last " + days + " day(s), this portfolio recorded " + buyCount + " buy(s) and "
            + sellCount + " sell(s), resulting in " + flowDescription + ". The most active asset was "
            + mostActiveAsset + ".";
    }
}
