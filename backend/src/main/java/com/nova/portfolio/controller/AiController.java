package com.nova.portfolio.controller;

import com.nova.portfolio.dto.ActivitySummaryResponse;
import com.nova.portfolio.dto.InvestmentAdviceResponse;
import com.nova.portfolio.dto.PortfolioForecastResponse;
import com.nova.portfolio.dto.PortfolioQaRequest;
import com.nova.portfolio.dto.PortfolioQaResponse;
import com.nova.portfolio.dto.TransactionAnomalyResponse;
import com.nova.portfolio.service.InvestmentAdviceService;
import com.nova.portfolio.service.PortfolioActivitySummaryService;
import com.nova.portfolio.service.PortfolioForecastService;
import com.nova.portfolio.service.PortfolioQaService;
import com.nova.portfolio.service.TransactionAnomalyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/portfolios/{portfolioId}")
public class AiController {

    private final InvestmentAdviceService investmentAdviceService;
    private final PortfolioForecastService portfolioForecastService;
    private final TransactionAnomalyService transactionAnomalyService;
    private final PortfolioActivitySummaryService portfolioActivitySummaryService;
    private final PortfolioQaService portfolioQaService;

    public AiController(
        InvestmentAdviceService investmentAdviceService,
        PortfolioForecastService portfolioForecastService,
        TransactionAnomalyService transactionAnomalyService,
        PortfolioActivitySummaryService portfolioActivitySummaryService,
        PortfolioQaService portfolioQaService
    ) {
        this.investmentAdviceService = investmentAdviceService;
        this.portfolioForecastService = portfolioForecastService;
        this.transactionAnomalyService = transactionAnomalyService;
        this.portfolioActivitySummaryService = portfolioActivitySummaryService;
        this.portfolioQaService = portfolioQaService;
    }

    @GetMapping("/advice")
    public InvestmentAdviceResponse advice(@PathVariable Long portfolioId) {
        return investmentAdviceService.generateAdvice(portfolioId);
    }

    @GetMapping("/forecast")
    public PortfolioForecastResponse forecast(
        @PathVariable Long portfolioId,
        @RequestParam(defaultValue = "12") int horizonMonths
    ) {
        return portfolioForecastService.forecast(portfolioId, horizonMonths);
    }

    @GetMapping("/anomalies")
    public List<TransactionAnomalyResponse> anomalies(@PathVariable Long portfolioId) {
        return transactionAnomalyService.detect(portfolioId);
    }

    @GetMapping("/summary")
    public ActivitySummaryResponse summary(
        @PathVariable Long portfolioId,
        @RequestParam(defaultValue = "30") int days
    ) {
        return portfolioActivitySummaryService.summarize(portfolioId, days);
    }

    @PostMapping("/query")
    public PortfolioQaResponse query(@PathVariable Long portfolioId, @Valid @RequestBody PortfolioQaRequest request) {
        return portfolioQaService.ask(portfolioId, request.getQuestion());
    }
}
