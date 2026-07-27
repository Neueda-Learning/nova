package com.nova.portfolio.mapper;

import com.nova.portfolio.dto.BondRequest;
import com.nova.portfolio.dto.BondResponse;
import com.nova.portfolio.model.Bond;

public final class BondMapper {

    private BondMapper() {
    }

    public static Bond toEntity(BondRequest request) {
        Bond bond = new Bond();
        bond.setName(request.getName());
        bond.setBondType(request.getBondType());
        bond.setIssuer(request.getIssuer());
        bond.setInterestRate(request.getInterestRate());
        bond.setMaturityDate(request.getMaturityDate());
        bond.setCurrentPrice(request.getCurrentPrice());
        bond.setRiskLevel(request.getRiskLevel());
        return bond;
    }

    public static void updateEntity(Bond entity, BondRequest request) {
        entity.setName(request.getName());
        entity.setBondType(request.getBondType());
        entity.setIssuer(request.getIssuer());
        entity.setInterestRate(request.getInterestRate());
        entity.setMaturityDate(request.getMaturityDate());
        entity.setCurrentPrice(request.getCurrentPrice());
        entity.setRiskLevel(request.getRiskLevel());
    }

    public static BondResponse toResponse(Bond entity) {
        BondResponse response = new BondResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setBondType(entity.getBondType());
        response.setIssuer(entity.getIssuer());
        response.setInterestRate(entity.getInterestRate());
        response.setMaturityDate(entity.getMaturityDate());
        response.setCurrentPrice(entity.getCurrentPrice());
        response.setRiskLevel(entity.getRiskLevel());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}

