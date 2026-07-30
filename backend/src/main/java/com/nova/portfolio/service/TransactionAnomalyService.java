package com.nova.portfolio.service;

import com.nova.portfolio.dto.TransactionAnomalyResponse;

import java.util.List;

public interface TransactionAnomalyService {

    List<TransactionAnomalyResponse> detect(Long portfolioId);
}
