package com.nova.portfolio.service;

import com.nova.portfolio.dto.ActivitySummaryResponse;

public interface PortfolioActivitySummaryService {

    ActivitySummaryResponse summarize(Long portfolioId, int days);
}
