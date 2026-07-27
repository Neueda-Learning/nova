package com.nova.portfolio.mapper;

import com.nova.portfolio.dto.BondResponse;
import com.nova.portfolio.model.Bond;

public final class BondMapper {

    private BondMapper() {
    }

    public static BondResponse toResponse(Bond bond) {
        BondResponse response = new BondResponse();
        response.setId(bond.getId());
        response.setName(bond.getName());
        response.setBondType(bond.getBondType());
        response.setIssuer(bond.getIssuer());
        response.setInterestRate(bond.getInterestRate());
        response.setMaturityDate(bond.getMaturityDate());
        response.setCurrentPrice(bond.getCurrentPrice());
        response.setRiskLevel(bond.getRiskLevel());
        return response;
    }
}
