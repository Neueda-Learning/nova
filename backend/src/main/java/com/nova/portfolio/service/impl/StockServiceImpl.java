package com.nova.portfolio.service.impl;

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
    public List<StockResponse> findAll() {
        return stockRepository.findAll().stream().map(StockMapper::toResponse).toList();
    }

    @Override
    public StockResponse findById(Long id) {
        Stock stock = stockRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Stock not found for id: " + id));
        return StockMapper.toResponse(stock);
    }
}
