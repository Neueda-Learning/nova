package com.nova.portfolio.service;

import com.nova.portfolio.dto.PortfolioResponse;

import java.util.List;

public interface PortfolioService {

    List<PortfolioResponse> findAll();

    PortfolioResponse findById(Long id);
}
