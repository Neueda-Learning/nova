package com.nova.portfolio.controller;

import com.nova.portfolio.dto.BondResponse;
import com.nova.portfolio.service.BondService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.nova.portfolio.dto.BondRequest;
import com.nova.portfolio.dto.BondResponse;
import com.nova.portfolio.service.BondService;
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
@RequestMapping("/api/bonds")
public class BondController {

    private final BondService bondService;

    public BondController(BondService bondService) {
        this.bondService = bondService;
    }

    @PostMapping
    public ResponseEntity<BondResponse> create(@Valid @RequestBody BondRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bondService.create(request));
    }

    @GetMapping
    public List<BondResponse> findAll() {
        return bondService.findAll();
    }

    @GetMapping("/{id}")
    public BondResponse findById(@PathVariable Long id) {
        return bondService.findById(id);
    }
}

    @PutMapping("/{id}")
    public BondResponse update(@PathVariable Long id, @Valid @RequestBody BondRequest request) {
        return bondService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bondService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

