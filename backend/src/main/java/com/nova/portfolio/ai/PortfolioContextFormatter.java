package com.nova.portfolio.ai;

import com.nova.portfolio.dto.AssetAllocationSliceResponse;
import com.nova.portfolio.dto.PortfolioHoldingSummaryResponse;
import com.nova.portfolio.dto.PortfolioSummaryResponse;
import com.nova.portfolio.dto.TransactionResponse;

import java.util.List;

/** Renders portfolio data into compact plain-text grounding context for LLM prompts. */
public final class PortfolioContextFormatter {

    private PortfolioContextFormatter() {
    }

    public static String formatSummary(PortfolioSummaryResponse summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("Portfolio: ").append(summary.getPortfolioName()).append('\n');
        sb.append("Total value: ").append(summary.getTotalValue()).append('\n');
        sb.append("Total unrealized P&L: ").append(summary.getTotalPnl()).append('\n');

        sb.append("Allocation:\n");
        for (AssetAllocationSliceResponse slice : summary.getAllocation()) {
            sb.append("  - ").append(slice.getLabel()).append(": ").append(slice.getValue())
                .append(" (").append(slice.getPercent()).append("%)\n");
        }

        sb.append("Holdings:\n");
        for (PortfolioHoldingSummaryResponse holding : summary.getHoldings()) {
            sb.append("  - ").append(holding.getAssetLabel()).append(" [").append(holding.getAssetType()).append(']')
                .append(": qty=").append(holding.getQuantity())
                .append(", avgCost=").append(holding.getAverageCost())
                .append(", unitPrice=").append(holding.getUnitPrice())
                .append(", marketValue=").append(holding.getMarketValue())
                .append(", unrealizedPnl=").append(holding.getUnrealizedPnl())
                .append('\n');
        }
        return sb.toString();
    }

    public static String formatTransactions(List<TransactionResponse> transactions) {
        if (transactions.isEmpty()) {
            return "No transactions in the selected period.";
        }
        StringBuilder sb = new StringBuilder("Recent transactions:\n");
        for (TransactionResponse transaction : transactions) {
            sb.append("  - ").append(transaction.getTransactionDate())
                .append(' ').append(transaction.getTransactionType())
                .append(' ').append(transaction.getAssetType())
                .append(" #").append(transaction.getAssetId())
                .append(": qty=").append(transaction.getQuantity())
                .append(" @ price=").append(transaction.getPrice())
                .append('\n');
        }
        return sb.toString();
    }
}
