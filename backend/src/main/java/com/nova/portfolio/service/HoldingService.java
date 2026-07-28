package com.nova.portfolio.service;

import com.nova.portfolio.dto.HoldingRequest;
import com.nova.portfolio.dto.HoldingResponse;

import java.util.List;

public interface HoldingService {

    HoldingResponse create(HoldingRequest request);

    List<HoldingResponse> findAll();

    List<HoldingResponse> findByPortfolioId(Long portfolioId);

    HoldingResponse findById(Long id);

    HoldingResponse update(Long id, HoldingRequest request);

    void delete(Long id);
}
