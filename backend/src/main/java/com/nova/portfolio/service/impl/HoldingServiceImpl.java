package com.nova.portfolio.service.impl;

import com.nova.portfolio.dto.HoldingRequest;
import com.nova.portfolio.dto.HoldingResponse;
import com.nova.portfolio.exception.ResourceNotFoundException;
import com.nova.portfolio.mapper.HoldingMapper;
import com.nova.portfolio.model.Holding;
import com.nova.portfolio.repository.HoldingRepository;
import com.nova.portfolio.service.HoldingService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HoldingServiceImpl implements HoldingService {

    private final HoldingRepository holdingRepository;

    public HoldingServiceImpl(HoldingRepository holdingRepository) {
        this.holdingRepository = holdingRepository;
    }

    @Override
    public HoldingResponse create(HoldingRequest request) {
        Holding saved = holdingRepository.save(HoldingMapper.toEntity(request));
        return HoldingMapper.toResponse(saved);
    }

    @Override
    public List<HoldingResponse> findAll() {
        return holdingRepository.findAll().stream().map(HoldingMapper::toResponse).toList();
    }

    @Override
    public HoldingResponse findById(Long id) {
        Holding holding = holdingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Holding not found for id: " + id));
        return HoldingMapper.toResponse(holding);
    }

    @Override
    public HoldingResponse update(Long id, HoldingRequest request) {
        Holding existing = holdingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Holding not found for id: " + id));
        HoldingMapper.updateEntity(existing, request);
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
}
