package com.nova.portfolio.service;

import com.nova.portfolio.dto.StockRequest;
import com.nova.portfolio.dto.StockResponse;

import java.util.List;

public interface StockService {

    StockResponse create(StockRequest request);

    List<StockResponse> findAll();

    StockResponse findById(Long id);

    StockResponse update(Long id, StockRequest request);

    void delete(Long id);
}

