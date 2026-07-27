package com.nova.portfolio.controller;

import com.nova.portfolio.dto.CashAssetResponse;
import com.nova.portfolio.service.CashAssetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.nova.portfolio.dto.CashAssetRequest;
import com.nova.portfolio.dto.CashAssetResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cash-assets")
public class CashAssetController {

    private final CashAssetService cashAssetService;

    public CashAssetController(CashAssetService cashAssetService) {
        this.cashAssetService = cashAssetService;
    }

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

