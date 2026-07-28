package com.nova.portfolio.mapper;

import com.nova.portfolio.dto.PortfolioRequest;
import com.nova.portfolio.dto.PortfolioResponse;
import com.nova.portfolio.model.Portfolio;

public final class PortfolioMapper {

    private PortfolioMapper() {
    }

    public static Portfolio toEntity(PortfolioRequest request) {
        Portfolio portfolio = new Portfolio();
        portfolio.setPortfolioName(request.getPortfolioName());
        portfolio.setDescription(request.getDescription());
        return portfolio;
    }

    public static void updateEntity(Portfolio entity, PortfolioRequest request) {
        entity.setPortfolioName(request.getPortfolioName());
        entity.setDescription(request.getDescription());
    }

    public static PortfolioResponse toResponse(Portfolio entity) {
        PortfolioResponse response = new PortfolioResponse();
        response.setId(entity.getId());
        response.setPortfolioName(entity.getPortfolioName());
        response.setDescription(entity.getDescription());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}

