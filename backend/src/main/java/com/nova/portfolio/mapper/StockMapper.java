package com.nova.portfolio.mapper;

import com.nova.portfolio.dto.StockResponse;
import com.nova.portfolio.model.Stock;

public final class StockMapper {

    private StockMapper() {
    }

    public static StockResponse toResponse(Stock stock) {
        StockResponse response = new StockResponse();
        response.setId(stock.getId());
        response.setSymbol(stock.getSymbol());
        response.setName(stock.getName());
        response.setSector(stock.getSector());
        response.setExchange(stock.getExchange());
        response.setPrice(stock.getPrice());
        return response;
    }
}
