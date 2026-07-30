package com.nova.portfolio.service;

import com.nova.portfolio.dto.PortfolioQaResponse;

public interface PortfolioQaService {

    PortfolioQaResponse ask(Long portfolioId, String question);
}
