package com.nova.portfolio.controller;

import com.nova.portfolio.dto.CashAssetRequest;
import com.nova.portfolio.dto.CashAssetResponse;
import com.nova.portfolio.marketdata.client.OpenExchangeRatesClient;
import com.nova.portfolio.service.CashAssetService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cash-assets")
public class CashAssetController {

    private final CashAssetService cashAssetService;
    private final OpenExchangeRatesClient oxrClient;

    public CashAssetController(CashAssetService cashAssetService, OpenExchangeRatesClient oxrClient) {
        this.cashAssetService = cashAssetService;
        this.oxrClient = oxrClient;
    }

    // ── OXR helper endpoints (used by Cash Assets form) ──────────────────────

    /** Returns { "USD": "United States Dollar", "EUR": "Euro", ... } */
    @GetMapping("/currencies")
    public ResponseEntity<Map<String, Object>> getCurrencies() {
        return ResponseEntity.ok(oxrClient.getCurrencies(false, false));
    }

    /**
     * Returns the exchange rate for the given currency code relative to USD.
     * e.g. /api/cash-assets/rate?currency=EUR → { "currency": "EUR", "rate": 0.9234, "base": "USD" }
     */
    @GetMapping("/rate")
    public ResponseEntity<Map<String, Object>> getRate(@RequestParam String currency) {
        String code = currency.trim().toUpperCase();
        Map<String, Object> latest = oxrClient.getLatest(null, code, null);
        @SuppressWarnings("unchecked")
        Map<String, Object> rates = (Map<String, Object>) latest.get("rates");
        Map<String, Object> result = new HashMap<>();
        result.put("currency", code);
        result.put("base", latest.getOrDefault("base", "USD"));
        if (rates != null && rates.containsKey(code)) {
            result.put("rate", rates.get(code));
        } else {
            result.put("rate", null);
        }
        return ResponseEntity.ok(result);
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<CashAssetResponse> create(@Valid @RequestBody CashAssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cashAssetService.create(request));
    }

    @GetMapping
    public List<CashAssetResponse> findAll() {
        return cashAssetService.findAll();
    }

    @GetMapping("/{id}")
    public CashAssetResponse findById(@PathVariable Long id) {
        return cashAssetService.findById(id);
    }

    @PutMapping("/{id}")
    public CashAssetResponse update(@PathVariable Long id, @Valid @RequestBody CashAssetRequest request) {
        return cashAssetService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cashAssetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

