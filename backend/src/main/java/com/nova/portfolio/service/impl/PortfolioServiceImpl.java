package com.nova.portfolio.service.impl;

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
