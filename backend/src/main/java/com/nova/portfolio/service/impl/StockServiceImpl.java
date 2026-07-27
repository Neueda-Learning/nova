package com.nova.portfolio.service.impl;

import com.nova.portfolio.dto.StockRequest;
import com.nova.portfolio.dto.StockResponse;
import com.nova.portfolio.exception.ResourceNotFoundException;
import com.nova.portfolio.mapper.StockMapper;
import com.nova.portfolio.model.Stock;
import com.nova.portfolio.repository.StockRepository;
import com.nova.portfolio.service.StockService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;

    public StockServiceImpl(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public StockResponse create(StockRequest request) {
        if (stockRepository.existsBySymbol(request.getSymbol())) {
            throw new IllegalArgumentException("Stock symbol already exists");
        }
        Stock saved = stockRepository.save(StockMapper.toEntity(request));
        return StockMapper.toResponse(saved);
    }

    @Override
    public List<StockResponse> findAll() {
        return stockRepository.findAll().stream().map(StockMapper::toResponse).toList();
    }

    @Override
    public StockResponse findById(Long id) {
        Stock stock = stockRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Stock not found for id: " + id));
        return StockMapper.toResponse(stock);
    }

    @Override
    public StockResponse update(Long id, StockRequest request) {
        Stock existing = stockRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Stock not found for id: " + id));

        boolean duplicateSymbol = stockRepository.existsBySymbol(request.getSymbol());
        boolean sameSymbol = existing.getSymbol().equals(request.getSymbol());
        if (duplicateSymbol && !sameSymbol) {
            throw new IllegalArgumentException("Stock symbol already exists");
        }

        StockMapper.updateEntity(existing, request);
        Stock saved = stockRepository.save(existing);
        return StockMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        if (!stockRepository.existsById(id)) {
            throw new ResourceNotFoundException("Stock not found for id: " + id);
        }
        stockRepository.deleteById(id);
    }
}

