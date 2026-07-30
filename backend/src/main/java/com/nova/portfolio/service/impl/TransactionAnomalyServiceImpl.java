package com.nova.portfolio.service.impl;

import com.nova.portfolio.dto.AnomalySeverity;
import com.nova.portfolio.dto.TransactionAnomalyResponse;
import com.nova.portfolio.exception.ResourceNotFoundException;
import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.model.Bond;
import com.nova.portfolio.model.CashAsset;
import com.nova.portfolio.model.Stock;
import com.nova.portfolio.model.Transaction;
import com.nova.portfolio.repository.BondRepository;
import com.nova.portfolio.repository.CashAssetRepository;
import com.nova.portfolio.repository.PortfolioRepository;
import com.nova.portfolio.repository.StockRepository;
import com.nova.portfolio.repository.TransactionRepository;
import com.nova.portfolio.service.TransactionAnomalyService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class TransactionAnomalyServiceImpl implements TransactionAnomalyService {

    private static final BigDecimal PRICE_DEVIATION_MEDIUM = new BigDecimal("0.20");
    private static final BigDecimal PRICE_DEVIATION_HIGH = new BigDecimal("0.50");
    private static final double ZSCORE_MEDIUM = 2.0;
    private static final double ZSCORE_HIGH = 3.0;
    private static final int MIN_SAMPLE_SIZE = 5;

    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final StockRepository stockRepository;
    private final BondRepository bondRepository;
    private final CashAssetRepository cashAssetRepository;

    public TransactionAnomalyServiceImpl(
        TransactionRepository transactionRepository,
        PortfolioRepository portfolioRepository,
        StockRepository stockRepository,
        BondRepository bondRepository,
        CashAssetRepository cashAssetRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.portfolioRepository = portfolioRepository;
        this.stockRepository = stockRepository;
        this.bondRepository = bondRepository;
        this.cashAssetRepository = cashAssetRepository;
    }

    @Override
    public List<TransactionAnomalyResponse> detect(Long portfolioId) {
        if (!portfolioRepository.existsById(portfolioId)) {
            throw new ResourceNotFoundException("Portfolio not found for id: " + portfolioId);
        }

        List<Transaction> transactions = transactionRepository.findByPortfolioIdOrderByTransactionDateDescIdDesc(portfolioId);
        NotionalStats stats = computeStats(transactions);

        List<TransactionAnomalyResponse> anomalies = new ArrayList<>();
        for (Transaction transaction : transactions) {
            List<String> reasons = new ArrayList<>();
            AnomalySeverity severity = maxSeverity(
                checkPriceDeviation(transaction, reasons),
                checkStatisticalOutlier(transaction, stats, reasons)
            );

            if (severity != null) {
                anomalies.add(toResponse(transaction, reasons, severity));
            }
        }
        return anomalies;
    }

    private AnomalySeverity checkPriceDeviation(Transaction transaction, List<String> reasons) {
        BigDecimal referencePrice = resolveReferencePrice(transaction.getAssetType(), transaction.getAssetId());
        if (referencePrice == null || referencePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal pctDiff = transaction.getPrice().subtract(referencePrice).abs()
            .divide(referencePrice, 4, RoundingMode.HALF_UP);

        if (pctDiff.compareTo(PRICE_DEVIATION_HIGH) > 0) {
            reasons.add("Price is " + percentLabel(pctDiff) + " away from the current reference price (" + referencePrice + ")");
            return AnomalySeverity.HIGH;
        }
        if (pctDiff.compareTo(PRICE_DEVIATION_MEDIUM) > 0) {
            reasons.add("Price is " + percentLabel(pctDiff) + " away from the current reference price (" + referencePrice + ")");
            return AnomalySeverity.MEDIUM;
        }
        return null;
    }

    private AnomalySeverity checkStatisticalOutlier(Transaction transaction, NotionalStats stats, List<String> reasons) {
        if (stats == null) {
            return null;
        }
        double z = (notionalOf(transaction) - stats.mean()) / stats.stdDev();

        if (Math.abs(z) > ZSCORE_HIGH) {
            reasons.add("Trade size is a statistical outlier vs this portfolio's history (z=" + round(z) + ")");
            return AnomalySeverity.HIGH;
        }
        if (Math.abs(z) > ZSCORE_MEDIUM) {
            reasons.add("Trade size is a statistical outlier vs this portfolio's history (z=" + round(z) + ")");
            return AnomalySeverity.MEDIUM;
        }
        return null;
    }

    private NotionalStats computeStats(List<Transaction> transactions) {
        if (transactions.size() < MIN_SAMPLE_SIZE) {
            return null;
        }
        double[] notionals = transactions.stream().mapToDouble(this::notionalOf).toArray();
        double mean = Arrays.stream(notionals).average().orElse(0.0);
        double variance = Arrays.stream(notionals).map(v -> (v - mean) * (v - mean)).sum() / notionals.length;
        double stdDev = Math.sqrt(variance);
        return stdDev == 0.0 ? null : new NotionalStats(mean, stdDev);
    }

    private double notionalOf(Transaction transaction) {
        return transaction.getQuantity().multiply(transaction.getPrice()).doubleValue();
    }

    private BigDecimal resolveReferencePrice(AssetType assetType, Long assetId) {
        return switch (assetType) {
            case STOCK -> stockRepository.findById(assetId).map(Stock::getPrice).orElse(null);
            case BOND -> bondRepository.findById(assetId).map(Bond::getCurrentPrice).orElse(null);
            case CASH -> cashAssetRepository.findById(assetId).map(CashAsset::getExchangeRate).orElse(null);
        };
    }

    private AnomalySeverity maxSeverity(AnomalySeverity a, AnomalySeverity b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    private String percentLabel(BigDecimal pctDiff) {
        return pctDiff.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP) + "%";
    }

    private String round(double value) {
        return String.format("%.2f", value);
    }

    private TransactionAnomalyResponse toResponse(Transaction transaction, List<String> reasons, AnomalySeverity severity) {
        TransactionAnomalyResponse response = new TransactionAnomalyResponse();
        response.setTransactionId(transaction.getId());
        response.setPortfolioId(transaction.getPortfolio().getId());
        response.setAssetType(transaction.getAssetType());
        response.setAssetId(transaction.getAssetId());
        response.setTransactionType(transaction.getTransactionType());
        response.setQuantity(transaction.getQuantity());
        response.setPrice(transaction.getPrice());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setReasons(reasons);
        response.setSeverity(severity);
        return response;
    }

    private record NotionalStats(double mean, double stdDev) {
    }
}
