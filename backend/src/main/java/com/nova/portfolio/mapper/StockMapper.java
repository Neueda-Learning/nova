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
        stock.setName(normalizeOptional(request.getName()));
        stock.setSector(normalizeOptional(request.getSector()));
        stock.setExchange(normalizeOptional(request.getExchange()));
        stock.setPrice(request.getPrice());
        stock.setMarketCap(request.getMarketCap());
        return stock;
    }

    public static void updateEntity(Stock entity, StockRequest request) {
        entity.setSymbol(request.getSymbol());
        entity.setName(normalizeOptional(request.getName()));
        entity.setSector(normalizeOptional(request.getSector()));
        entity.setExchange(normalizeOptional(request.getExchange()));
        entity.setPrice(request.getPrice());
        entity.setMarketCap(request.getMarketCap());
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static StockResponse toResponse(Stock entity) {
        StockResponse response = new StockResponse();
        response.setId(entity.getId());
        response.setSymbol(entity.getSymbol());
        response.setName(entity.getName());
        response.setSector(entity.getSector());
        response.setExchange(entity.getExchange());
        response.setPrice(entity.getPrice());
        response.setMarketCap(entity.getMarketCap());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}

