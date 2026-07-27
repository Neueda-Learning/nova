package com.nova.portfolio.mapper;

import com.nova.portfolio.dto.CashAssetRequest;
import com.nova.portfolio.dto.CashAssetResponse;
import com.nova.portfolio.model.CashAsset;

public final class CashAssetMapper {

    private CashAssetMapper() {
    }

    public static CashAsset toEntity(CashAssetRequest request) {
        CashAsset asset = new CashAsset();
        asset.setCurrency(request.getCurrency());
        asset.setExchangeRate(request.getExchangeRate());
        return asset;
    }

    public static void updateEntity(CashAsset entity, CashAssetRequest request) {
        entity.setCurrency(request.getCurrency());
        entity.setExchangeRate(request.getExchangeRate());
    }

    public static CashAssetResponse toResponse(CashAsset entity) {
        CashAssetResponse response = new CashAssetResponse();
        response.setId(entity.getId());
        response.setCurrency(entity.getCurrency());
        response.setExchangeRate(entity.getExchangeRate());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}

