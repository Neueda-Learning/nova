package com.nova.portfolio.repository;

import com.nova.portfolio.model.CashAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashAssetRepository extends JpaRepository<CashAsset, Long> {
}

