package com.nova.portfolio.service;

import com.nova.portfolio.dto.TransactionRequest;
import com.nova.portfolio.dto.TransactionResponse;

import java.util.List;

public interface TransactionService {

    TransactionResponse create(TransactionRequest request);

    List<TransactionResponse> findAll();

    List<TransactionResponse> findByPortfolioId(Long portfolioId);

    TransactionResponse findById(Long id);

    void delete(Long id);
}
