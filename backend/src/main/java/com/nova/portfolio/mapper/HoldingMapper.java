package com.nova.portfolio.mapper;

import com.nova.portfolio.dto.HoldingRequest;
import com.nova.portfolio.dto.HoldingResponse;
import com.nova.portfolio.model.Holding;
import com.nova.portfolio.model.Portfolio;

public final class HoldingMapper {

    private HoldingMapper() {
    }

    public static Holding toEntity(HoldingRequest request, Portfolio portfolio) {
        Holding holding = new Holding();
        holding.setPortfolio(portfolio);
        holding.setAssetType(request.getAssetType());
        holding.setAssetId(request.getAssetId());
        holding.setQuantity(request.getQuantity());
        holding.setAverageCost(request.getAverageCost());
        return holding;
    }

    public static void updateEntity(Holding entity, HoldingRequest request, Portfolio portfolio) {
        entity.setPortfolio(portfolio);
        entity.setAssetType(request.getAssetType());
        entity.setAssetId(request.getAssetId());
        entity.setQuantity(request.getQuantity());
        entity.setAverageCost(request.getAverageCost());
    }

    public static HoldingResponse toResponse(Holding holding) {
        HoldingResponse response = new HoldingResponse();
        response.setId(holding.getId());
        response.setPortfolioId(holding.getPortfolio().getId());
        response.setAssetType(holding.getAssetType());
        response.setAssetId(holding.getAssetId());
        response.setQuantity(holding.getQuantity());
        response.setAverageCost(holding.getAverageCost());
        response.setCreatedAt(holding.getCreatedAt());
        response.setUpdatedAt(holding.getUpdatedAt());
        return response;
    }
}
