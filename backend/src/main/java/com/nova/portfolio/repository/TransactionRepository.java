package com.nova.portfolio.repository;

import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByAssetTypeAndAssetId(AssetType assetType, Long assetId);

    List<Transaction> findByPortfolioIdOrderByTransactionDateDescIdDesc(Long portfolioId);

    void deleteByPortfolioId(Long portfolioId);
}
