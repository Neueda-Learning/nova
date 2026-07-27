package com.nova.portfolio.controller;

import com.nova.portfolio.dto.CashAssetResponse;
import com.nova.portfolio.service.CashAssetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cash-assets")
public class CashAssetController {

    private final CashAssetService cashAssetService;

    public CashAssetController(CashAssetService cashAssetService) {
        this.cashAssetService = cashAssetService;
    }

    @GetMapping
    public List<CashAssetResponse> findAll() {
        return cashAssetService.findAll();
    }

    @GetMapping("/{id}")
    public CashAssetResponse findById(@PathVariable Long id) {
        return cashAssetService.findById(id);
    }
}
