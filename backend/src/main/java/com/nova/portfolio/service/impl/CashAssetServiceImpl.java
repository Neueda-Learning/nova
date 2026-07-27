package com.nova.portfolio.service.impl;

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
    public List<CashAssetResponse> findAll() {
        return cashAssetRepository.findAll().stream().map(CashAssetMapper::toResponse).toList();
    }

    @Override
    public CashAssetResponse findById(Long id) {
        CashAsset cashAsset = cashAssetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("CashAsset not found for id: " + id));
        return CashAssetMapper.toResponse(cashAsset);
    }
}
