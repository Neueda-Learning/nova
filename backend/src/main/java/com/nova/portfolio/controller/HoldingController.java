package com.nova.portfolio.controller;

import com.nova.portfolio.dto.HoldingRequest;
import com.nova.portfolio.dto.HoldingResponse;
import com.nova.portfolio.service.HoldingService;
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
@RequestMapping("/api/holdings")
public class HoldingController {

    private final HoldingService holdingService;

    public HoldingController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    @PostMapping
    public ResponseEntity<HoldingResponse> create(@Valid @RequestBody HoldingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holdingService.create(request));
    }

    @GetMapping
    public List<HoldingResponse> findAll() {
        return holdingService.findAll();
    }

    @GetMapping("/portfolio/{portfolioId}")
    public List<HoldingResponse> findByPortfolioId(@PathVariable Long portfolioId) {
        return holdingService.findByPortfolioId(portfolioId);
    }

    @GetMapping("/{id}")
    public HoldingResponse findById(@PathVariable Long id) {
        return holdingService.findById(id);
    }

    @PutMapping("/{id}")
    public HoldingResponse update(@PathVariable Long id, @Valid @RequestBody HoldingRequest request) {
        return holdingService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        holdingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
