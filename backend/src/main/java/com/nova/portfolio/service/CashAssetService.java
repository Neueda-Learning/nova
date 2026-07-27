package com.nova.portfolio.service;

import com.nova.portfolio.dto.CashAssetRequest;
import com.nova.portfolio.dto.CashAssetResponse;

import java.util.List;

public interface CashAssetService {

    List<CashAssetResponse> findAll();

    CashAssetResponse findById(Long id);
}
    CashAssetResponse create(CashAssetRequest request);

    List<CashAssetResponse> findAll();

    CashAssetResponse findById(Long id);

    CashAssetResponse update(Long id, CashAssetRequest request);

    void delete(Long id);
}

