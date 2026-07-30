package com.nova.portfolio.service;

import com.nova.portfolio.dto.PortfolioForecastResponse;

public interface PortfolioForecastService {

    PortfolioForecastResponse forecast(Long portfolioId, int horizonMonths);
}
