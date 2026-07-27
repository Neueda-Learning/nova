package com.nova.portfolio.service.impl;

import com.nova.portfolio.dto.PortfolioRequest;
import com.nova.portfolio.dto.PortfolioResponse;
import com.nova.portfolio.exception.ResourceNotFoundException;
import com.nova.portfolio.mapper.PortfolioMapper;
import com.nova.portfolio.model.Portfolio;
import com.nova.portfolio.repository.PortfolioRepository;
import com.nova.portfolio.service.PortfolioService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;

    public PortfolioServiceImpl(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @Override
    public PortfolioResponse create(PortfolioRequest request) {
        if (portfolioRepository.existsByPortfolioName(request.getPortfolioName())) {
            throw new IllegalArgumentException("Portfolio name already exists");
        }
        Portfolio saved = portfolioRepository.save(PortfolioMapper.toEntity(request));
        return PortfolioMapper.toResponse(saved);
    }

    @Override
    public List<PortfolioResponse> findAll() {
        return portfolioRepository.findAll().stream().map(PortfolioMapper::toResponse).toList();
    }

    @Override
    public PortfolioResponse findById(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for id: " + id));
        return PortfolioMapper.toResponse(portfolio);
    }
}

    @Override
    public PortfolioResponse update(Long id, PortfolioRequest request) {
        Portfolio existing = portfolioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for id: " + id));

        boolean duplicateName = portfolioRepository.existsByPortfolioName(request.getPortfolioName());
        boolean sameName = existing.getPortfolioName().equals(request.getPortfolioName());
        if (duplicateName && !sameName) {
            throw new IllegalArgumentException("Portfolio name already exists");
        }

        PortfolioMapper.updateEntity(existing, request);
        Portfolio saved = portfolioRepository.save(existing);
        return PortfolioMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        if (!portfolioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Portfolio not found for id: " + id);
        }
        portfolioRepository.deleteById(id);
    }
}

