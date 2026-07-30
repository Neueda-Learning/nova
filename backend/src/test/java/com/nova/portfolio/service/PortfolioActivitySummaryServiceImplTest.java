package com.nova.portfolio.service;

import com.nova.portfolio.ai.client.LlmClient;
import com.nova.portfolio.dto.ActivitySummaryResponse;
import com.nova.portfolio.dto.AiResponseSource;
import com.nova.portfolio.dto.PortfolioSummaryResponse;
import com.nova.portfolio.exception.AiServiceException;
import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.model.Portfolio;
import com.nova.portfolio.model.Transaction;
import com.nova.portfolio.model.TransactionType;
import com.nova.portfolio.repository.TransactionRepository;
import com.nova.portfolio.service.impl.PortfolioActivitySummaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioActivitySummaryServiceImpl Tests")
class PortfolioActivitySummaryServiceImplTest {

    @Mock
    private PortfolioAnalyticsService portfolioAnalyticsService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LlmClient llmClient;

    @InjectMocks
    private PortfolioActivitySummaryServiceImpl portfolioActivitySummaryService;

    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio();
        portfolio.setId(1L);
        portfolio.setPortfolioName("Growth Portfolio");
    }

    private Transaction transaction(long id, AssetType assetType, long assetId, TransactionType type, String quantity, String price) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setPortfolio(portfolio);
        transaction.setAssetType(assetType);
        transaction.setAssetId(assetId);
        transaction.setTransactionType(type);
        transaction.setQuantity(new BigDecimal(quantity));
        transaction.setPrice(new BigDecimal(price));
        transaction.setTransactionDate(LocalDate.now().minusDays(id));
        return transaction;
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should reject an out-of-range days value")
        void shouldRejectOutOfRangeDays() {
            assertThatThrownBy(() -> portfolioActivitySummaryService.summarize(1L, 0))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> portfolioActivitySummaryService.summarize(1L, 366))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Activity computation")
    class ActivityComputationTests {

        @Test
        @DisplayName("Should return a no-activity message when there are no transactions")
        void shouldReturnNoActivityMessage() {
            when(portfolioAnalyticsService.getPortfolioSummary(1L)).thenReturn(new PortfolioSummaryResponse());
            when(transactionRepository.findByPortfolioIdAndTransactionDateGreaterThanEqualOrderByTransactionDateDescIdDesc(
                eq(1L), any(LocalDate.class))).thenReturn(List.of());

            ActivitySummaryResponse result = portfolioActivitySummaryService.summarize(1L, 30);

            assertThat(result.getSource()).isEqualTo(AiResponseSource.RULE_BASED);
            assertThat(result.getBuyCount()).isZero();
            assertThat(result.getSellCount()).isZero();
            assertThat(result.getSummary()).contains("No transactions");
        }

        @Test
        @DisplayName("Should compute buy/sell counts, net cash flow, and the most active asset")
        void shouldComputeActivityStats() {
            List<Transaction> transactions = List.of(
                transaction(1L, AssetType.STOCK, 1L, TransactionType.BUY, "10", "100"),
                transaction(2L, AssetType.STOCK, 1L, TransactionType.SELL, "5", "110"),
                transaction(3L, AssetType.BOND, 2L, TransactionType.BUY, "2", "50")
            );

            when(portfolioAnalyticsService.getPortfolioSummary(1L)).thenReturn(new PortfolioSummaryResponse());
            when(transactionRepository.findByPortfolioIdAndTransactionDateGreaterThanEqualOrderByTransactionDateDescIdDesc(
                eq(1L), any(LocalDate.class))).thenReturn(transactions);
            when(llmClient.isAvailable()).thenReturn(false);

            ActivitySummaryResponse result = portfolioActivitySummaryService.summarize(1L, 30);

            assertThat(result.getSource()).isEqualTo(AiResponseSource.RULE_BASED);
            assertThat(result.getBuyCount()).isEqualTo(2);
            assertThat(result.getSellCount()).isEqualTo(1);
            assertThat(result.getNetCashFlow()).isEqualByComparingTo(new BigDecimal("-550"));
            assertThat(result.getMostActiveAsset()).isEqualTo("STOCK #1");
            assertThat(result.getSummary()).contains("2 buy(s)").contains("1 sell(s)");
        }

        @Test
        @DisplayName("Should use the LLM narrative when available")
        void shouldUseLlmNarrative() {
            List<Transaction> transactions = List.of(
                transaction(1L, AssetType.STOCK, 1L, TransactionType.BUY, "10", "100")
            );

            when(portfolioAnalyticsService.getPortfolioSummary(1L)).thenReturn(new PortfolioSummaryResponse());
            when(transactionRepository.findByPortfolioIdAndTransactionDateGreaterThanEqualOrderByTransactionDateDescIdDesc(
                eq(1L), any(LocalDate.class))).thenReturn(transactions);
            when(llmClient.isAvailable()).thenReturn(true);
            when(llmClient.complete(anyString(), anyString())).thenReturn("AI narrative");

            ActivitySummaryResponse result = portfolioActivitySummaryService.summarize(1L, 30);

            assertThat(result.getSource()).isEqualTo(AiResponseSource.AI);
            assertThat(result.getSummary()).isEqualTo("AI narrative");
        }

        @Test
        @DisplayName("Should degrade to a rule-based summary when the LLM call fails")
        void shouldDegradeWhenLlmThrows() {
            List<Transaction> transactions = List.of(
                transaction(1L, AssetType.STOCK, 1L, TransactionType.BUY, "10", "100")
            );

            when(portfolioAnalyticsService.getPortfolioSummary(1L)).thenReturn(new PortfolioSummaryResponse());
            when(transactionRepository.findByPortfolioIdAndTransactionDateGreaterThanEqualOrderByTransactionDateDescIdDesc(
                eq(1L), any(LocalDate.class))).thenReturn(transactions);
            when(llmClient.isAvailable()).thenReturn(true);
            when(llmClient.complete(anyString(), anyString())).thenThrow(new AiServiceException("boom", 500));

            ActivitySummaryResponse result = portfolioActivitySummaryService.summarize(1L, 30);

            assertThat(result.getSource()).isEqualTo(AiResponseSource.RULE_BASED);
            assertThat(result.getSummary()).contains("1 buy(s)");
        }
    }
}
