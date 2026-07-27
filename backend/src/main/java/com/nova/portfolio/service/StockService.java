package com.nova.portfolio.service;

import com.nova.portfolio.dto.StockResponse;

import java.util.List;

public interface StockService {

    List<StockResponse> findAll();

    StockResponse findById(Long id);
}
