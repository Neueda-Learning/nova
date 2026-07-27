package com.nova.portfolio.repository;

import com.nova.portfolio.model.Bond;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BondRepository extends JpaRepository<Bond, Long> {
}

