package com.nova.portfolio.service;

import com.nova.portfolio.ai.client.LlmClient;
import com.nova.portfolio.dto.AiResponseSource;
import com.nova.portfolio.dto.PortfolioQaResponse;
import com.nova.portfolio.dto.PortfolioSummaryResponse;
import com.nova.portfolio.exception.AiServiceException;
import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.model.Portfolio;
import com.nova.portfolio.model.Transaction;
import com.nova.portfolio.model.TransactionType;
import com.nova.portfolio.repository.TransactionRepository;
import com.nova.portfolio.service.impl.PortfolioQaServiceImpl;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioQaServiceImpl Tests")
class PortfolioQaServiceImplTest {

    @Mock
    private PortfolioAnalyticsService portfolioAnalyticsService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LlmClient llmClient;

    @InjectMocks
    private PortfolioQaServiceImpl portfolioQaService;

    @Nested
    @DisplayName("Availability gating")
    class AvailabilityTests {

        @Test
        @DisplayName("Should throw AiServiceException when the LLM is not configured")
        void shouldThrowWhenUnavailable() {
            when(llmClient.isAvailable()).thenReturn(false);

            assertThatThrownBy(() -> portfolioQaService.ask(1L, "What is my balance?"))
                .isInstanceOf(AiServiceException.class)
                .satisfies(ex -> assertThat(((AiServiceException) ex).getStatusCode()).isEqualTo(503));

            verify(portfolioAnalyticsService, never()).getPortfolioSummary(anyLong());
        }
    }

    @Nested
    @DisplayName("Question answering")
    class QuestionAnsweringTests {

        @Test
        @DisplayName("Should answer using the LLM when available")
        void shouldAnswerWhenAvailable() {
            Portfolio portfolio = new Portfolio();
            portfolio.setId(1L);

            Transaction transaction = new Transaction();
            transaction.setId(1L);
            transaction.setPortfolio(portfolio);
            transaction.setAssetType(AssetType.STOCK);
            transaction.setAssetId(1L);
            transaction.setTransactionType(TransactionType.BUY);
            transaction.setQuantity(new BigDecimal("10"));
            transaction.setPrice(new BigDecimal("100"));
            transaction.setTransactionDate(LocalDate.now());

            when(llmClient.isAvailable()).thenReturn(true);
            when(portfolioAnalyticsService.getPortfolioSummary(1L)).thenReturn(new PortfolioSummaryResponse());
            when(transactionRepository.findByPortfolioIdOrderByTransactionDateDescIdDesc(1L))
                .thenReturn(List.of(transaction));
            when(llmClient.complete(anyString(), eq("What is my balance?"))).thenReturn("42 dollars");

            PortfolioQaResponse result = portfolioQaService.ask(1L, "What is my balance?");

            assertThat(result.getSource()).isEqualTo(AiResponseSource.AI);
            assertThat(result.getAnswer()).isEqualTo("42 dollars");
            assertThat(result.getQuestion()).isEqualTo("What is my balance?");
            assertThat(result.getPortfolioId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should propagate the failure when the LLM call fails")
        void shouldPropagateLlmFailure() {
            when(llmClient.isAvailable()).thenReturn(true);
            when(portfolioAnalyticsService.getPortfolioSummary(1L)).thenReturn(new PortfolioSummaryResponse());
            when(transactionRepository.findByPortfolioIdOrderByTransactionDateDescIdDesc(1L)).thenReturn(List.of());
            when(llmClient.complete(anyString(), anyString())).thenThrow(new AiServiceException("boom", 502));

            assertThatThrownBy(() -> portfolioQaService.ask(1L, "What is my balance?"))
                .isInstanceOf(AiServiceException.class);
        }
    }
}
