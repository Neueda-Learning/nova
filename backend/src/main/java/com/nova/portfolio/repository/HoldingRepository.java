package com.nova.portfolio.repository;

import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    boolean existsByPortfolioIdAndAssetTypeAndAssetId(Long portfolioId, AssetType assetType, Long assetId);

    boolean existsByAssetTypeAndAssetId(AssetType assetType, Long assetId);

    Optional<Holding> findByPortfolioIdAndAssetTypeAndAssetId(Long portfolioId, AssetType assetType, Long assetId);

    List<Holding> findByPortfolioId(Long portfolioId);

    void deleteByPortfolioId(Long portfolioId);
}
