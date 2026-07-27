package com.nova.portfolio.service;

import com.nova.portfolio.dto.BondResponse;

import java.util.List;

public interface BondService {

    List<BondResponse> findAll();

    BondResponse findById(Long id);
}
