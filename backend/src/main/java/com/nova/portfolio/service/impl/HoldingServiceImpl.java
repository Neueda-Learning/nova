package com.nova.portfolio.service.impl;

import com.nova.portfolio.dto.HoldingRequest;
import com.nova.portfolio.dto.HoldingResponse;
import com.nova.portfolio.exception.ResourceNotFoundException;
import com.nova.portfolio.mapper.HoldingMapper;
import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.model.Holding;
import com.nova.portfolio.model.Portfolio;
import com.nova.portfolio.repository.BondRepository;
import com.nova.portfolio.repository.CashAssetRepository;
import com.nova.portfolio.repository.HoldingRepository;
import com.nova.portfolio.repository.PortfolioRepository;
import com.nova.portfolio.repository.StockRepository;
import com.nova.portfolio.service.HoldingService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HoldingServiceImpl implements HoldingService {

    private final HoldingRepository holdingRepository;
    private final PortfolioRepository portfolioRepository;
    private final StockRepository stockRepository;
    private final BondRepository bondRepository;
    private final CashAssetRepository cashAssetRepository;

    public HoldingServiceImpl(
        HoldingRepository holdingRepository,
        PortfolioRepository portfolioRepository,
        StockRepository stockRepository,
        BondRepository bondRepository,
        CashAssetRepository cashAssetRepository
    ) {
        this.holdingRepository = holdingRepository;
        this.portfolioRepository = portfolioRepository;
        this.stockRepository = stockRepository;
        this.bondRepository = bondRepository;
        this.cashAssetRepository = cashAssetRepository;
    }

    @Override
    public HoldingResponse create(HoldingRequest request) {
        Portfolio portfolio = getPortfolioOrThrow(request.getPortfolioId());
        validateAssetExists(request.getAssetType(), request.getAssetId());
        if (holdingRepository.existsByPortfolioIdAndAssetTypeAndAssetId(
            request.getPortfolioId(), request.getAssetType(), request.getAssetId()
        )) {
            throw new IllegalArgumentException("Holding already exists for this portfolio and asset");
        }

        Holding saved = holdingRepository.save(HoldingMapper.toEntity(request, portfolio));
        return HoldingMapper.toResponse(saved);
    }

    @Override
    public List<HoldingResponse> findAll() {
        return holdingRepository.findAll().stream().map(HoldingMapper::toResponse).toList();
    }

    @Override
    public List<HoldingResponse> findByPortfolioId(Long portfolioId) {
        getPortfolioOrThrow(portfolioId);
        return holdingRepository.findByPortfolioId(portfolioId).stream().map(HoldingMapper::toResponse).toList();
    }

    @Override
    public HoldingResponse findById(Long id) {
        Holding holding = holdingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Holding not found for id: " + id));
        return HoldingMapper.toResponse(holding);
    }

    @Override
    public List<HoldingResponse> findByPortfolioId(Long portfolioId) {
        return holdingRepository.findByPortfolioId(portfolioId).stream().map(HoldingMapper::toResponse).toList();
    }

    @Override
    public HoldingResponse update(Long id, HoldingRequest request) {
        Holding existing = holdingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Holding not found for id: " + id));

        Portfolio portfolio = getPortfolioOrThrow(request.getPortfolioId());
        validateAssetExists(request.getAssetType(), request.getAssetId());

        boolean duplicate = holdingRepository.existsByPortfolioIdAndAssetTypeAndAssetId(
            request.getPortfolioId(), request.getAssetType(), request.getAssetId()
        );
        boolean sameAssetIdentity = existing.getPortfolio().getId().equals(request.getPortfolioId())
            && existing.getAssetType() == request.getAssetType()
            && existing.getAssetId().equals(request.getAssetId());

        if (duplicate && !sameAssetIdentity) {
            throw new IllegalArgumentException("Holding already exists for this portfolio and asset");
        }

        HoldingMapper.updateEntity(existing, request, portfolio);
        Holding saved = holdingRepository.save(existing);
        return HoldingMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        if (!holdingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Holding not found for id: " + id);
        }
        holdingRepository.deleteById(id);
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
