package com.nova.portfolio.controller;

import com.nova.portfolio.dto.StockRequest;
import com.nova.portfolio.dto.StockResponse;
import com.nova.portfolio.service.StockService;
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
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping
    public ResponseEntity<StockResponse> create(@Valid @RequestBody StockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.create(request));
    }

    @GetMapping
    public List<StockResponse> findAll() {
        return stockService.findAll();
    }

    @GetMapping("/{id}")
    public StockResponse findById(@PathVariable Long id) {
        return stockService.findById(id);
    }

    @PutMapping("/{id}")
    public StockResponse update(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        return stockService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stockService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

