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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldServeForecastAndAnomaliesEndToEnd() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long portfolioId = createPortfolio("Growth-" + suffix);
        Long stockId = createStock("AAPL-" + suffix, new BigDecimal("100.00"));
        createHolding(portfolioId, stockId, new BigDecimal("10.0000"), new BigDecimal("90.0000"));
        createTransaction(portfolioId, stockId, "BUY", new BigDecimal("10"), new BigDecimal("90.00"));

        mockMvc.perform(get("/api/ai/portfolios/{id}/forecast", portfolioId).param("horizonMonths", "6"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.portfolioId").value(portfolioId))
            .andExpect(jsonPath("$.points").isArray())
            .andExpect(jsonPath("$.points.length()").value(6))
            .andExpect(jsonPath("$.disclaimer").isString());

        mockMvc.perform(get("/api/ai/portfolios/{id}/anomalies", portfolioId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldRejectOutOfRangeForecastHorizon() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long portfolioId = createPortfolio("Income-" + suffix);

        mockMvc.perform(get("/api/ai/portfolios/{id}/forecast", portfolioId).param("horizonMonths", "0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnRuleBasedAdviceAndSummaryWithoutAiKey() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long portfolioId = createPortfolio("Balanced-" + suffix);
        Long stockId = createStock("MSFT-" + suffix, new BigDecimal("50.00"));
        createHolding(portfolioId, stockId, new BigDecimal("5.0000"), new BigDecimal("45.0000"));

        mockMvc.perform(get("/api/ai/portfolios/{id}/advice", portfolioId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.source").value("RULE_BASED"))
            .andExpect(jsonPath("$.disclaimer").isString());

        mockMvc.perform(get("/api/ai/portfolios/{id}/summary", portfolioId).param("days", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.source").value("RULE_BASED"));
    }

    @Test
    void shouldReturn503ForQueryWithoutAiKey() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long portfolioId = createPortfolio("Query-" + suffix);

        Map<String, Object> payload = new HashMap<>();
        payload.put("question", "What is my total value?");

        mockMvc.perform(post("/api/ai/portfolios/{id}/query", portfolioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value(503))
            .andExpect(jsonPath("$.message").isString());
    }

    private Long createPortfolio(String portfolioName) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("portfolioName", portfolioName);

        String response = mockMvc.perform(post("/api/portfolios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createStock(String symbol, BigDecimal price) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("symbol", symbol);
        payload.put("price", price);

        String response = mockMvc.perform(post("/api/stocks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private void createHolding(
        Long portfolioId,
        Long assetId,
        BigDecimal quantity,
        BigDecimal averageCost
    ) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("portfolioId", portfolioId);
        payload.put("assetType", AssetType.STOCK.name());
        payload.put("assetId", assetId);
        payload.put("quantity", quantity);
        payload.put("averageCost", averageCost);

        mockMvc.perform(post("/api/holdings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated());
    }

    private void createTransaction(
        Long portfolioId,
        Long assetId,
        String transactionType,
        BigDecimal quantity,
        BigDecimal price
    ) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("portfolioId", portfolioId);
        payload.put("assetType", AssetType.STOCK.name());
        payload.put("assetId", assetId);
        payload.put("transactionType", transactionType);
        payload.put("quantity", quantity);
        payload.put("price", price);
        payload.put("transactionDate", LocalDate.now().toString());

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated());
    }
}
