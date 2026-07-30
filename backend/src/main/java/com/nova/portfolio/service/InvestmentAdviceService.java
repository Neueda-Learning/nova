package com.nova.portfolio.service;

import com.nova.portfolio.dto.InvestmentAdviceResponse;

public interface InvestmentAdviceService {

    InvestmentAdviceResponse generateAdvice(Long portfolioId);
}
