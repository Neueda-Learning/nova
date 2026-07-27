package com.nova.portfolio.service.impl;

import com.nova.portfolio.dto.CashAssetRequest;
import com.nova.portfolio.dto.CashAssetResponse;
import com.nova.portfolio.exception.ResourceNotFoundException;
import com.nova.portfolio.mapper.CashAssetMapper;
import com.nova.portfolio.model.CashAsset;
import com.nova.portfolio.repository.CashAssetRepository;
import com.nova.portfolio.service.CashAssetService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CashAssetServiceImpl implements CashAssetService {

    private final CashAssetRepository cashAssetRepository;

    public CashAssetServiceImpl(CashAssetRepository cashAssetRepository) {
        this.cashAssetRepository = cashAssetRepository;
    }

    @Override
    public CashAssetResponse create(CashAssetRequest request) {
        if (cashAssetRepository.existsByCurrency(request.getCurrency())) {
            throw new IllegalArgumentException("Cash currency already exists");
        }
        CashAsset saved = cashAssetRepository.save(CashAssetMapper.toEntity(request));
        return CashAssetMapper.toResponse(saved);
    }

    @Override
    public List<CashAssetResponse> findAll() {
        return cashAssetRepository.findAll().stream().map(CashAssetMapper::toResponse).toList();
    }

    @Override
    public CashAssetResponse findById(Long id) {
        CashAsset asset = cashAssetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cash asset not found for id: " + id));
        return CashAssetMapper.toResponse(asset);
    }

    @Override
    public CashAssetResponse update(Long id, CashAssetRequest request) {
        CashAsset existing = cashAssetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cash asset not found for id: " + id));

        boolean duplicateCurrency = cashAssetRepository.existsByCurrency(request.getCurrency());
        boolean sameCurrency = existing.getCurrency().equals(request.getCurrency());
        if (duplicateCurrency && !sameCurrency) {
            throw new IllegalArgumentException("Cash currency already exists");
        }

        CashAssetMapper.updateEntity(existing, request);
        CashAsset saved = cashAssetRepository.save(existing);
        return CashAssetMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        if (!cashAssetRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cash asset not found for id: " + id);
        }
        cashAssetRepository.deleteById(id);
    }
}

