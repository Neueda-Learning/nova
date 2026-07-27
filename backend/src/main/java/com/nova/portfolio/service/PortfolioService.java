package com.nova.portfolio.service;

import com.nova.portfolio.dto.PortfolioRequest;
import com.nova.portfolio.dto.PortfolioResponse;

import java.util.List;

public interface PortfolioService {

    PortfolioResponse create(PortfolioRequest request);

    List<PortfolioResponse> findAll();

    PortfolioResponse findById(Long id);

    PortfolioResponse update(Long id, PortfolioRequest request);

    void delete(Long id);
}

