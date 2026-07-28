package com.nova.portfolio.controller;

import com.nova.portfolio.dto.HoldingResponse;
import com.nova.portfolio.dto.PortfolioDashboardResponse;
import com.nova.portfolio.dto.PortfolioRequest;
import com.nova.portfolio.dto.PortfolioResponse;
import com.nova.portfolio.dto.PortfolioSummaryResponse;
import com.nova.portfolio.service.PortfolioAnalyticsService;
import com.nova.portfolio.service.HoldingService;
import com.nova.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final HoldingService holdingService;
    private final PortfolioAnalyticsService portfolioAnalyticsService;

    public PortfolioController(
        PortfolioService portfolioService,
        HoldingService holdingService,
        PortfolioAnalyticsService portfolioAnalyticsService
    ) {
        this.portfolioService = portfolioService;
        this.holdingService = holdingService;
        this.portfolioAnalyticsService = portfolioAnalyticsService;
    }

    @PostMapping
    public ResponseEntity<PortfolioResponse> create(@Valid @RequestBody PortfolioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolioService.create(request));
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

    @GetMapping("/dashboard")
    public PortfolioDashboardResponse dashboard() {
        return portfolioAnalyticsService.getDashboardSummary();
    }

    @GetMapping("/{id}/summary")
    public PortfolioSummaryResponse summary(@PathVariable Long id) {
        return portfolioAnalyticsService.getPortfolioSummary(id);
    }

    @PutMapping("/{id}")
    public PortfolioResponse update(@PathVariable Long id, @Valid @RequestBody PortfolioRequest request) {
        return portfolioService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        portfolioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

