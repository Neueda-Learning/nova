package com.nova.portfolio.service.impl;

import com.nova.portfolio.dto.BondRequest;
import com.nova.portfolio.dto.BondResponse;
import com.nova.portfolio.exception.ResourceNotFoundException;
import com.nova.portfolio.mapper.BondMapper;
import com.nova.portfolio.model.Bond;
import com.nova.portfolio.repository.BondRepository;
import com.nova.portfolio.service.BondService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BondServiceImpl implements BondService {

    private final BondRepository bondRepository;

    public BondServiceImpl(BondRepository bondRepository) {
        this.bondRepository = bondRepository;
    }

    @Override
    public BondResponse create(BondRequest request) {
        Bond saved = bondRepository.save(BondMapper.toEntity(request));
        return BondMapper.toResponse(saved);
    }

    @Override
    public List<BondResponse> findAll() {
        return bondRepository.findAll().stream().map(BondMapper::toResponse).toList();
    }

    @Override
    public BondResponse findById(Long id) {
        Bond bond = bondRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Bond not found for id: " + id));
        return BondMapper.toResponse(bond);
    }
}

    @Override
    public BondResponse update(Long id, BondRequest request) {
        Bond existing = bondRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Bond not found for id: " + id));

        BondMapper.updateEntity(existing, request);
        Bond saved = bondRepository.save(existing);
        return BondMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        if (!bondRepository.existsById(id)) {
            throw new ResourceNotFoundException("Bond not found for id: " + id);
        }
        bondRepository.deleteById(id);
    }
}

