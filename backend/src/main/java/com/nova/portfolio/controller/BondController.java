package com.nova.portfolio.controller;

import com.nova.portfolio.dto.BondResponse;
import com.nova.portfolio.service.BondService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping
    public List<BondResponse> findAll() {
        return bondService.findAll();
    }

    @GetMapping("/{id}")
    public BondResponse findById(@PathVariable Long id) {
        return bondService.findById(id);
    }
}
