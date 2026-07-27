package com.nova.portfolio.repository;

import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    boolean existsByPortfolioIdAndAssetTypeAndAssetId(Long portfolioId, AssetType assetType, Long assetId);

    List<Holding> findByPortfolioId(Long portfolioId);
}
