package com.nova.portfolio.service.impl;

import com.nova.portfolio.dto.TransactionRequest;
import com.nova.portfolio.dto.TransactionResponse;
import com.nova.portfolio.exception.ResourceNotFoundException;
import com.nova.portfolio.mapper.TransactionMapper;
import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.model.Holding;
import com.nova.portfolio.model.Portfolio;
import com.nova.portfolio.model.Transaction;
import com.nova.portfolio.model.TransactionType;
import com.nova.portfolio.repository.BondRepository;
import com.nova.portfolio.repository.CashAssetRepository;
import com.nova.portfolio.repository.HoldingRepository;
import com.nova.portfolio.repository.PortfolioRepository;
import com.nova.portfolio.repository.StockRepository;
import com.nova.portfolio.repository.TransactionRepository;
import com.nova.portfolio.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final HoldingRepository holdingRepository;
    private final PortfolioRepository portfolioRepository;
    private final StockRepository stockRepository;
    private final BondRepository bondRepository;
    private final CashAssetRepository cashAssetRepository;

    public TransactionServiceImpl(
        TransactionRepository transactionRepository,
        HoldingRepository holdingRepository,
        PortfolioRepository portfolioRepository,
        StockRepository stockRepository,
        BondRepository bondRepository,
        CashAssetRepository cashAssetRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.holdingRepository = holdingRepository;
        this.portfolioRepository = portfolioRepository;
        this.stockRepository = stockRepository;
        this.bondRepository = bondRepository;
        this.cashAssetRepository = cashAssetRepository;
    }

    @Override
    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        Portfolio portfolio = getPortfolioOrThrow(request.getPortfolioId());
        validateAssetExists(request.getAssetType(), request.getAssetId());

        applyToHolding(portfolio, request);

        Transaction saved = transactionRepository.save(TransactionMapper.toEntity(request, portfolio));
        return TransactionMapper.toResponse(saved);
    }

    @Override
    public List<TransactionResponse> findAll() {
        return transactionRepository.findAll().stream().map(TransactionMapper::toResponse).toList();
    }

    @Override
    public List<TransactionResponse> findByPortfolioId(Long portfolioId) {
        getPortfolioOrThrow(portfolioId);
        return transactionRepository.findByPortfolioIdOrderByTransactionDateDescIdDesc(portfolioId).stream()
            .map(TransactionMapper::toResponse)
            .toList();
    }

    @Override
    public TransactionResponse findById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for id: " + id));
        return TransactionMapper.toResponse(transaction);
    }

    @Override
    public void delete(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Transaction not found for id: " + id);
        }
        transactionRepository.deleteById(id);
    }

    /**
     * Applies the BUY/SELL effect of this transaction to the underlying holding.
     * BUY: creates the holding if absent, otherwise accumulates quantity and
     * recomputes a quantity-weighted average cost.
     * SELL: requires an existing holding with sufficient quantity; removes the
     * holding entirely if the sale brings it down to zero. Average cost is left
     * unchanged by sells (weighted-average-cost method).
     */
    private void applyToHolding(Portfolio portfolio, TransactionRequest request) {
        Optional<Holding> existingOpt = holdingRepository.findByPortfolioIdAndAssetTypeAndAssetId(
            portfolio.getId(), request.getAssetType(), request.getAssetId()
        );

        if (request.getTransactionType() == TransactionType.BUY) {
            if (existingOpt.isPresent()) {
                Holding holding = existingOpt.get();
                BigDecimal existingCost = holding.getAverageCost() != null ? holding.getAverageCost() : BigDecimal.ZERO;
                BigDecimal existingTotalCost = holding.getQuantity().multiply(existingCost);
                BigDecimal incomingTotalCost = request.getQuantity().multiply(request.getPrice());
                BigDecimal newQuantity = holding.getQuantity().add(request.getQuantity());
                BigDecimal newAverageCost = existingTotalCost.add(incomingTotalCost)
                    .divide(newQuantity, 4, RoundingMode.HALF_UP);
                holding.setQuantity(newQuantity);
                holding.setAverageCost(newAverageCost);
                holdingRepository.save(holding);
            } else {
                Holding holding = new Holding();
                holding.setPortfolio(portfolio);
                holding.setAssetType(request.getAssetType());
                holding.setAssetId(request.getAssetId());
                holding.setQuantity(request.getQuantity());
                holding.setAverageCost(request.getPrice());
                holdingRepository.save(holding);
            }
        } else {
            Holding holding = existingOpt.orElseThrow(() ->
                new IllegalArgumentException("Cannot sell: no existing holding for this asset in the portfolio"));
            if (holding.getQuantity().compareTo(request.getQuantity()) < 0) {
                throw new IllegalArgumentException("Cannot sell: insufficient holding quantity");
            }
            BigDecimal newQuantity = holding.getQuantity().subtract(request.getQuantity());
            if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
                holdingRepository.delete(holding);
            } else {
                holding.setQuantity(newQuantity);
                holdingRepository.save(holding);
            }
        }
    }

    private Portfolio getPortfolioOrThrow(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for id: " + portfolioId));
    }

    private void validateAssetExists(AssetType assetType, Long assetId) {
        boolean exists = switch (assetType) {
            case STOCK -> stockRepository.existsById(assetId);
            case BOND -> bondRepository.existsById(assetId);
            case CASH -> cashAssetRepository.existsById(assetId);
        };

        if (!exists) {
            throw new ResourceNotFoundException(assetType + " not found for id: " + assetId);
        }
    }
}
