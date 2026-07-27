package com.nova.portfolio.service;

import com.nova.portfolio.dto.CashAssetResponse;

import java.util.List;

public interface CashAssetService {

    List<CashAssetResponse> findAll();

    CashAssetResponse findById(Long id);
}
