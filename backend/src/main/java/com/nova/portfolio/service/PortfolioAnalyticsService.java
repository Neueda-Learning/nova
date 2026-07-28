package com.nova.portfolio.service;

import com.nova.portfolio.dto.PortfolioDashboardResponse;
import com.nova.portfolio.dto.PortfolioSummaryResponse;

public interface PortfolioAnalyticsService {

    PortfolioDashboardResponse getDashboardSummary();

    PortfolioSummaryResponse getPortfolioSummary(Long portfolioId);
}

