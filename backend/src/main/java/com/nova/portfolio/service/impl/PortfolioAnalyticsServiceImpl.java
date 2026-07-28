package com.nova.portfolio.service.impl;

import com.nova.portfolio.dto.AssetAllocationSliceResponse;
import com.nova.portfolio.dto.PortfolioDashboardResponse;
import com.nova.portfolio.dto.PortfolioHoldingSummaryResponse;
import com.nova.portfolio.dto.PortfolioSummaryResponse;
import com.nova.portfolio.exception.ResourceNotFoundException;
import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.model.Bond;
import com.nova.portfolio.model.CashAsset;
import com.nova.portfolio.model.Holding;
import com.nova.portfolio.model.Portfolio;
import com.nova.portfolio.model.Stock;
import com.nova.portfolio.repository.BondRepository;
import com.nova.portfolio.repository.CashAssetRepository;
import com.nova.portfolio.repository.HoldingRepository;
import com.nova.portfolio.repository.PortfolioRepository;
import com.nova.portfolio.repository.StockRepository;
import com.nova.portfolio.service.PortfolioAnalyticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PortfolioAnalyticsServiceImpl implements PortfolioAnalyticsService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final Map<AssetType, String> ASSET_COLORS = Map.of(
        AssetType.STOCK, "#2563eb",
        AssetType.BOND, "#7c3aed",
        AssetType.CASH, "#059669"
    );

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;
    private final BondRepository bondRepository;
    private final CashAssetRepository cashAssetRepository;

    public PortfolioAnalyticsServiceImpl(
        PortfolioRepository portfolioRepository,
        HoldingRepository holdingRepository,
        StockRepository stockRepository,
        BondRepository bondRepository,
        CashAssetRepository cashAssetRepository
    ) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.stockRepository = stockRepository;
        this.bondRepository = bondRepository;
        this.cashAssetRepository = cashAssetRepository;
    }

    @Override
    public PortfolioDashboardResponse getDashboardSummary() {
        List<Portfolio> portfolios = portfolioRepository.findAll();
        List<Holding> allHoldings = holdingRepository.findAll();
        AssetLookup lookup = buildAssetLookup(allHoldings);

        List<PortfolioHoldingSummaryResponse> enrichedHoldings = allHoldings.stream()
            .map((holding) -> toHoldingSummary(holding, lookup))
            .toList();

        Map<Long, List<PortfolioHoldingSummaryResponse>> holdingsByPortfolio = enrichedHoldings.stream()
            .collect(Collectors.groupingBy(PortfolioHoldingSummaryResponse::getPortfolioId));

        List<PortfolioSummaryResponse> portfolioSummaries = portfolios.stream()
            .map((portfolio) -> toPortfolioSummary(portfolio, holdingsByPortfolio.getOrDefault(portfolio.getId(), List.of())))
            .toList();

        PortfolioDashboardResponse response = new PortfolioDashboardResponse();
        response.setPortfolios(portfolioSummaries);
        response.setAllocation(calculateAllocation(enrichedHoldings));
        response.setGlobalTotal(sumMarketValue(enrichedHoldings));
        return response;
    }

    @Override
    public PortfolioSummaryResponse getPortfolioSummary(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for id: " + portfolioId));

        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        AssetLookup lookup = buildAssetLookup(holdings);

        List<PortfolioHoldingSummaryResponse> enriched = holdings.stream()
            .map((holding) -> toHoldingSummary(holding, lookup))
            .toList();

        return toPortfolioSummary(portfolio, enriched);
    }

    private PortfolioSummaryResponse toPortfolioSummary(Portfolio portfolio, List<PortfolioHoldingSummaryResponse> holdings) {
        List<PortfolioHoldingSummaryResponse> sortedHoldings = new ArrayList<>(holdings);
        sortedHoldings.sort(Comparator.comparing(
            (PortfolioHoldingSummaryResponse item) -> item.getMarketValue() == null ? BigDecimal.ZERO : item.getMarketValue()
        ).reversed());

        boolean hasPnlData = sortedHoldings.stream().anyMatch((holding) -> holding.getUnrealizedPnl() != null);

        PortfolioSummaryResponse response = new PortfolioSummaryResponse();
        response.setId(portfolio.getId());
        response.setPortfolioName(portfolio.getPortfolioName());
        response.setDescription(portfolio.getDescription());
        response.setCreatedAt(portfolio.getCreatedAt());
        response.setUpdatedAt(portfolio.getUpdatedAt());
        response.setHoldings(sortedHoldings);
        response.setHoldingCount(sortedHoldings.size());
        response.setTotalValue(sumMarketValue(sortedHoldings));
        response.setTotalPnl(sumPnl(sortedHoldings));
        response.setHasPnlData(hasPnlData);
        response.setAllocation(calculateAllocation(sortedHoldings));
        return response;
    }

    private List<AssetAllocationSliceResponse> calculateAllocation(List<PortfolioHoldingSummaryResponse> holdings) {
        Map<AssetType, BigDecimal> totals = new EnumMap<>(AssetType.class);
        totals.put(AssetType.STOCK, BigDecimal.ZERO);
        totals.put(AssetType.BOND, BigDecimal.ZERO);
        totals.put(AssetType.CASH, BigDecimal.ZERO);

        for (PortfolioHoldingSummaryResponse holding : holdings) {
            if (holding.getMarketValue() == null) {
                continue;
            }
            totals.computeIfPresent(holding.getAssetType(), (key, current) -> current.add(holding.getMarketValue()));
        }

        BigDecimal grandTotal = totals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<AssetAllocationSliceResponse> slices = new ArrayList<>();

        for (AssetType assetType : List.of(AssetType.STOCK, AssetType.BOND, AssetType.CASH)) {
            BigDecimal value = totals.getOrDefault(assetType, BigDecimal.ZERO);
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            AssetAllocationSliceResponse slice = new AssetAllocationSliceResponse();
            slice.setLabel(assetType.name());
            slice.setColor(ASSET_COLORS.get(assetType));
            slice.setValue(value);
            if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
                slice.setPercent(value.multiply(HUNDRED).divide(grandTotal, 1, RoundingMode.HALF_UP));
            } else {
                slice.setPercent(BigDecimal.ZERO);
            }
            slices.add(slice);
        }

        return slices;
    }

    private BigDecimal sumMarketValue(List<PortfolioHoldingSummaryResponse> holdings) {
        return holdings.stream()
            .map(PortfolioHoldingSummaryResponse::getMarketValue)
            .filter((value) -> value != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumPnl(List<PortfolioHoldingSummaryResponse> holdings) {
        return holdings.stream()
            .map(PortfolioHoldingSummaryResponse::getUnrealizedPnl)
            .filter((value) -> value != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PortfolioHoldingSummaryResponse toHoldingSummary(Holding holding, AssetLookup lookup) {
        PriceAndLabel priceAndLabel = resolvePriceAndLabel(holding.getAssetType(), holding.getAssetId(), lookup);
        BigDecimal marketValue = null;
        if (priceAndLabel.unitPrice() != null && holding.getQuantity() != null) {
            marketValue = holding.getQuantity().multiply(priceAndLabel.unitPrice());
        }

        BigDecimal unrealizedPnl = null;
        if (marketValue != null && holding.getAverageCost() != null) {
            unrealizedPnl = marketValue.subtract(holding.getAverageCost().multiply(holding.getQuantity()));
        }

        PortfolioHoldingSummaryResponse response = new PortfolioHoldingSummaryResponse();
        response.setId(holding.getId());
        response.setPortfolioId(holding.getPortfolio().getId());
        response.setAssetType(holding.getAssetType());
        response.setAssetId(holding.getAssetId());
        response.setAssetLabel(priceAndLabel.label());
        response.setQuantity(holding.getQuantity());
        response.setAverageCost(holding.getAverageCost());
        response.setUnitPrice(priceAndLabel.unitPrice());
        response.setMarketValue(marketValue);
        response.setUnrealizedPnl(unrealizedPnl);
        response.setCreatedAt(holding.getCreatedAt());
        response.setUpdatedAt(holding.getUpdatedAt());
        return response;
    }

    private PriceAndLabel resolvePriceAndLabel(AssetType assetType, Long assetId, AssetLookup lookup) {
        if (assetType == AssetType.STOCK) {
            Stock stock = lookup.stocks().get(assetId);
            if (stock == null) {
                return new PriceAndLabel("#" + assetId, null);
            }
            return new PriceAndLabel(stock.getSymbol() + " - " + stock.getName(), stock.getPrice());
        }
        if (assetType == AssetType.BOND) {
            Bond bond = lookup.bonds().get(assetId);
            if (bond == null) {
                return new PriceAndLabel("#" + assetId, null);
            }
            return new PriceAndLabel(bond.getName() + " (" + bond.getIssuer() + ")", bond.getCurrentPrice());
        }

        CashAsset cashAsset = lookup.cashAssets().get(assetId);
        if (cashAsset == null) {
            return new PriceAndLabel("#" + assetId, null);
        }
        return new PriceAndLabel(cashAsset.getCurrency(), cashAsset.getExchangeRate());
    }

    private AssetLookup buildAssetLookup(List<Holding> holdings) {
        Set<Long> stockIds = new HashSet<>();
        Set<Long> bondIds = new HashSet<>();
        Set<Long> cashAssetIds = new HashSet<>();

        for (Holding holding : holdings) {
            if (holding.getAssetType() == AssetType.STOCK) {
                stockIds.add(holding.getAssetId());
            } else if (holding.getAssetType() == AssetType.BOND) {
                bondIds.add(holding.getAssetId());
            } else if (holding.getAssetType() == AssetType.CASH) {
                cashAssetIds.add(holding.getAssetId());
            }
        }

        Map<Long, Stock> stocks = new HashMap<>();
        for (Stock stock : stockRepository.findAllById(stockIds)) {
            stocks.put(stock.getId(), stock);
        }

        Map<Long, Bond> bonds = new HashMap<>();
        for (Bond bond : bondRepository.findAllById(bondIds)) {
            bonds.put(bond.getId(), bond);
        }

        Map<Long, CashAsset> cashAssets = new HashMap<>();
        for (CashAsset cashAsset : cashAssetRepository.findAllById(cashAssetIds)) {
            cashAssets.put(cashAsset.getId(), cashAsset);
        }

        return new AssetLookup(stocks, bonds, cashAssets);
    }

    private record AssetLookup(
        Map<Long, Stock> stocks,
        Map<Long, Bond> bonds,
        Map<Long, CashAsset> cashAssets
    ) {
    }

    private record PriceAndLabel(String label, BigDecimal unitPrice) {
    }
}

