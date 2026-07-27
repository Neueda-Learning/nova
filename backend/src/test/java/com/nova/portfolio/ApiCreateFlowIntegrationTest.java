package com.nova.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nova.portfolio.model.AssetType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiCreateFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreatePortfolioStockBondCashAndHolding() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Long portfolioId = createPortfolio("Growth-" + suffix);
        Long stockId = createStock("AAPL-" + suffix);
        createBond("US10Y-" + suffix);
        Long cashAssetId = createCashAsset("USD" + suffix);

        createHolding(portfolioId, AssetType.STOCK, stockId, new BigDecimal("10.5000"));
        createHolding(portfolioId, AssetType.CASH, cashAssetId, new BigDecimal("1000.0000"));
    }

    private Long createPortfolio(String portfolioName) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("portfolioName", portfolioName);

        String response = mockMvc.perform(post("/api/portfolios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.portfolioName").value(portfolioName))
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createStock(String symbol) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("symbol", symbol);
        payload.put("name", "Apple Inc");
        payload.put("sector", "Technology");
        payload.put("exchange", "NASDAQ");
        payload.put("price", new BigDecimal("210.7500"));

        String response = mockMvc.perform(post("/api/stocks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.symbol").value(symbol))
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private void createBond(String name) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("bondType", "Government");
        payload.put("issuer", "US Treasury");
        payload.put("interestRate", new BigDecimal("3.5000"));
        payload.put("maturityDate", LocalDate.now().plusYears(5).toString());
        payload.put("currentPrice", new BigDecimal("99.5000"));
        payload.put("riskLevel", "LOW");

        mockMvc.perform(post("/api/bonds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name").value(name));
    }

    private Long createCashAsset(String currency) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("currency", currency);
        payload.put("exchangeRate", new BigDecimal("1.000000"));

        String response = mockMvc.perform(post("/api/cash-assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.currency").value(currency))
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private void createHolding(Long portfolioId, AssetType assetType, Long assetId, BigDecimal quantity) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("portfolioId", portfolioId);
        payload.put("assetType", assetType.name());
        payload.put("assetId", assetId);
        payload.put("quantity", quantity);

        mockMvc.perform(post("/api/holdings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.portfolioId").value(portfolioId))
            .andExpect(jsonPath("$.assetType").value(assetType.name()))
            .andExpect(jsonPath("$.assetId").value(assetId));
    }
}

