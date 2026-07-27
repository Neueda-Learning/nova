package com.nova.portfolio.controller;

import com.nova.portfolio.dto.HoldingResponse;
import com.nova.portfolio.dto.PortfolioResponse;
import com.nova.portfolio.service.HoldingService;
import com.nova.portfolio.service.PortfolioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final HoldingService holdingService;

    public PortfolioController(PortfolioService portfolioService, HoldingService holdingService) {
        this.portfolioService = portfolioService;
        this.holdingService = holdingService;
    }

    @GetMapping
    public List<PortfolioResponse> findAll() {
        return portfolioService.findAll();
    }

    @GetMapping("/{id}")
    public PortfolioResponse findById(@PathVariable Long id) {
        return portfolioService.findById(id);
    }

    @GetMapping("/{id}/holdings")
    public List<HoldingResponse> findHoldings(@PathVariable Long id) {
        return holdingService.findByPortfolioId(id);
    }
}
