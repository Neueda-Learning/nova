package com.nova.portfolio.repository;

import com.nova.portfolio.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    boolean existsBySymbol(String symbol);
}
