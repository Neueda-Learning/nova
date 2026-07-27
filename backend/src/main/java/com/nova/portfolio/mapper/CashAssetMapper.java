package com.nova.portfolio.mapper;

import com.nova.portfolio.dto.CashAssetResponse;
import com.nova.portfolio.model.CashAsset;

public final class CashAssetMapper {

    private CashAssetMapper() {
    }

    public static CashAssetResponse toResponse(CashAsset cashAsset) {
        CashAssetResponse response = new CashAssetResponse();
        response.setId(cashAsset.getId());
        response.setCurrency(cashAsset.getCurrency());
        response.setExchangeRate(cashAsset.getExchangeRate());
        return response;
    }
}
