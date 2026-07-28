package com.nova.portfolio.mapper;

import com.nova.portfolio.dto.StockRequest;
import com.nova.portfolio.dto.StockResponse;
import com.nova.portfolio.model.Stock;

public final class StockMapper {

    private StockMapper() {
    }

    public static Stock toEntity(StockRequest request) {
        Stock stock = new Stock();
        stock.setSymbol(request.getSymbol());
        stock.setName(request.getName());
        stock.setSector(request.getSector());
        stock.setExchange(request.getExchange());
        stock.setPrice(request.getPrice());
        return stock;
    }

    public static void updateEntity(Stock entity, StockRequest request) {
        entity.setSymbol(request.getSymbol());
        entity.setName(request.getName());
        entity.setSector(request.getSector());
        entity.setExchange(request.getExchange());
        entity.setPrice(request.getPrice());
    }

    public static StockResponse toResponse(Stock entity) {
        StockResponse response = new StockResponse();
        response.setId(entity.getId());
        response.setSymbol(entity.getSymbol());
        response.setName(entity.getName());
        response.setSector(entity.getSector());
        response.setExchange(entity.getExchange());
        response.setPrice(entity.getPrice());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}

