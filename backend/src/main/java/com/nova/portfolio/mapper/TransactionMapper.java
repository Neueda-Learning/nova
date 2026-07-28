package com.nova.portfolio.mapper;

import com.nova.portfolio.dto.TransactionRequest;
import com.nova.portfolio.dto.TransactionResponse;
import com.nova.portfolio.model.Portfolio;
import com.nova.portfolio.model.Transaction;

public final class TransactionMapper {

    private TransactionMapper() {
    }

    public static Transaction toEntity(TransactionRequest request, Portfolio portfolio) {
        Transaction transaction = new Transaction();
        transaction.setPortfolio(portfolio);
        transaction.setAssetType(request.getAssetType());
        transaction.setAssetId(request.getAssetId());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setQuantity(request.getQuantity());
        transaction.setPrice(request.getPrice());
        transaction.setTransactionDate(request.getTransactionDate());
        return transaction;
    }

    public static TransactionResponse toResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setPortfolioId(transaction.getPortfolio().getId());
        response.setAssetType(transaction.getAssetType());
        response.setAssetId(transaction.getAssetId());
        response.setTransactionType(transaction.getTransactionType());
        response.setQuantity(transaction.getQuantity());
        response.setPrice(transaction.getPrice());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        return response;
    }
}
