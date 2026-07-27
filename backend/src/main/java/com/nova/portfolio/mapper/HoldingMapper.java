package com.nova.portfolio.mapper;

import com.nova.portfolio.dto.HoldingRequest;
import com.nova.portfolio.dto.HoldingResponse;
import com.nova.portfolio.model.Holding;

public final class HoldingMapper {

    private HoldingMapper() {
    }

    public static Holding toEntity(HoldingRequest request) {
        Holding holding = new Holding();
        holding.setStockTicker(request.getStockTicker().toUpperCase());
        holding.setVolume(request.getVolume());
        return holding;
    }

    public static void updateEntity(Holding entity, HoldingRequest request) {
        entity.setStockTicker(request.getStockTicker().toUpperCase());
        entity.setVolume(request.getVolume());
    }

    public static HoldingResponse toResponse(Holding holding) {
        HoldingResponse response = new HoldingResponse();
        response.setId(holding.getId());
        response.setStockTicker(holding.getStockTicker());
        response.setVolume(holding.getVolume());
        return response;
    }
}
