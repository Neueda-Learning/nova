package com.nova.portfolio.service;

import com.nova.portfolio.dto.BondRequest;
import com.nova.portfolio.dto.BondResponse;

import java.util.List;

public interface BondService {

    List<BondResponse> findAll();

    BondResponse findById(Long id);
}
    BondResponse create(BondRequest request);

    List<BondResponse> findAll();

    BondResponse findById(Long id);

    BondResponse update(Long id, BondRequest request);

    void delete(Long id);
}

