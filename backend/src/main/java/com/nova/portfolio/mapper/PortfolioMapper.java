package com.nova.portfolio.mapper;

import com.nova.portfolio.dto.PortfolioResponse;
import com.nova.portfolio.model.Portfolio;

public final class PortfolioMapper {

    private PortfolioMapper() {
    }

    public static PortfolioResponse toResponse(Portfolio portfolio) {
        PortfolioResponse response = new PortfolioResponse();
        response.setId(portfolio.getId());
        response.setPortfolioName(portfolio.getPortfolioName());
        response.setCreatedAt(portfolio.getCreatedAt());
        response.setUpdatedAt(portfolio.getUpdatedAt());
        return response;
    }
}
