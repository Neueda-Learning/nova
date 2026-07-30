package com.nova.portfolio.service;

import com.nova.portfolio.ai.client.LlmClient;
import com.nova.portfolio.dto.AiResponseSource;
import com.nova.portfolio.dto.AssetAllocationSliceResponse;
import com.nova.portfolio.dto.InvestmentAdviceResponse;
import com.nova.portfolio.dto.PortfolioHoldingSummaryResponse;
import com.nova.portfolio.dto.PortfolioSummaryResponse;
import com.nova.portfolio.exception.AiServiceException;
import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.service.impl.InvestmentAdviceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvestmentAdviceServiceImpl Tests")
class InvestmentAdviceServiceImplTest {

    @Mock
    private PortfolioAnalyticsService portfolioAnalyticsService;

    @Mock
    private LlmClient llmClient;

    @InjectMocks
    private InvestmentAdviceServiceImpl investmentAdviceService;

    private PortfolioSummaryResponse balancedSummary;

    @BeforeEach
    void setUp() {
        balancedSummary = new PortfolioSummaryResponse();
        balancedSummary.setTotalValue(new BigDecimal("10000"));
        balancedSummary.setAllocation(List.of(
            slice(AssetType.STOCK, "40"),
            slice(AssetType.BOND, "40"),
            slice(AssetType.CASH, "20")
        ));
        balancedSummary.setHoldings(List.of());
    }

    private AssetAllocationSliceResponse slice(AssetType assetType, String percent) {
        AssetAllocationSliceResponse slice = new AssetAllocationSliceResponse();
        slice.setLabel(assetType.name());
        slice.setPercent(new BigDecimal(percent));
        return slice;
    }

    private PortfolioHoldingSummaryResponse holding(String label, String marketValue, String unrealizedPnl) {
        PortfolioHoldingSummaryResponse holding = new PortfolioHoldingSummaryResponse();
        holding.setAssetLabel(label);
        holding.setMarketValue(new BigDecimal(marketValue));
        holding.setUnrealizedPnl(new BigDecimal(unrealizedPnl));
        return holding;
    }

    @Nested
    @DisplayName("AI availability branching")
    class AiAvailabilityTests {

        @Test
        @DisplayName("Should use the LLM response when available")
        void shouldUseLlmWhenAvailable() {
            when(portfolioAnalyticsService.getPortfolioSummary(1L)).thenReturn(balancedSummary);
            when(llmClient.isAvailable()).thenReturn(true);
            when(llmClient.complete(anyString(), anyString())).thenReturn("AI generated advice text");

            InvestmentAdviceResponse result = investmentAdviceService.generateAdvice(1L);

            assertThat(result.getSource()).isEqualTo(AiResponseSource.AI);
            assertThat(result.getAdvice()).isEqualTo("AI generated advice text");
            assertThat(result.getDisclaimer()).isNotBlank();
        }

        @Test
        @DisplayName("Should fall back to rule-based advice when the LLM is unavailable")
        void shouldFallBackWhenUnavailable() {
            PortfolioSummaryResponse concentratedSummary = new PortfolioSummaryResponse();
            concentratedSummary.setTotalValue(new BigDecimal("10000"));
            concentratedSummary.setAllocation(List.of(slice(AssetType.STOCK, "80")));
            concentratedSummary.setHoldings(List.of());

            when(portfolioAnalyticsService.getPortfolioSummary(2L)).thenReturn(concentratedSummary);
            when(llmClient.isAvailable()).thenReturn(false);

            InvestmentAdviceResponse result = investmentAdviceService.generateAdvice(2L);

            assertThat(result.getSource()).isEqualTo(AiResponseSource.RULE_BASED);
            assertThat(result.getSignals()).isNotEmpty();
            assertThat(result.getAdvice()).contains("STOCK");
            verify(llmClient, never()).complete(anyString(), anyString());
        }

        @Test
        @DisplayName("Should degrade to rule-based advice when the LLM call fails")
        void shouldDegradeWhenLlmThrows() {
            when(portfolioAnalyticsService.getPortfolioSummary(3L)).thenReturn(balancedSummary);
            when(llmClient.isAvailable()).thenReturn(true);
            when(llmClient.complete(anyString(), anyString())).thenThrow(new AiServiceException("boom", 500));

            InvestmentAdviceResponse result = investmentAdviceService.generateAdvice(3L);

            assertThat(result.getSource()).isEqualTo(AiResponseSource.RULE_BASED);
            assertThat(result.getSignals()).isEmpty();
            assertThat(result.getAdvice()).contains("does not currently trigger");
        }
    }

    @Nested
    @DisplayName("Signal detection")
    class SignalDetectionTests {

        @Test
        @DisplayName("Should detect concentration, idle cash, and large loss signals")
        void shouldDetectAllSignalTypes() {
            PortfolioSummaryResponse summary = new PortfolioSummaryResponse();
            summary.setTotalValue(new BigDecimal("10000"));
            summary.setAllocation(List.of(slice(AssetType.STOCK, "60"), slice(AssetType.CASH, "35")));
            summary.setHoldings(List.of(holding("AAPL", "700", "-300")));

            when(portfolioAnalyticsService.getPortfolioSummary(4L)).thenReturn(summary);
            when(llmClient.isAvailable()).thenReturn(false);

            InvestmentAdviceResponse result = investmentAdviceService.generateAdvice(4L);

            assertThat(result.getSignals()).hasSize(3);
            assertThat(result.getSignals()).anyMatch(s -> s.contains("STOCK"));
            assertThat(result.getSignals()).anyMatch(s -> s.contains("Cash"));
            assertThat(result.getSignals()).anyMatch(s -> s.contains("AAPL"));
        }
    }
}
