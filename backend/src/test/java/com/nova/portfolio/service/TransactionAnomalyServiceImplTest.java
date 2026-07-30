package com.nova.portfolio.service;

import com.nova.portfolio.dto.AnomalySeverity;
import com.nova.portfolio.dto.TransactionAnomalyResponse;
import com.nova.portfolio.exception.ResourceNotFoundException;
import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.model.CashAsset;
import com.nova.portfolio.model.Portfolio;
import com.nova.portfolio.model.Stock;
import com.nova.portfolio.model.Transaction;
import com.nova.portfolio.model.TransactionType;
import com.nova.portfolio.repository.CashAssetRepository;
import com.nova.portfolio.repository.PortfolioRepository;
import com.nova.portfolio.repository.StockRepository;
import com.nova.portfolio.repository.TransactionRepository;
import com.nova.portfolio.service.impl.TransactionAnomalyServiceImpl;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionAnomalyServiceImpl Tests")
class TransactionAnomalyServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private com.nova.portfolio.repository.BondRepository bondRepository;

    @Mock
    private CashAssetRepository cashAssetRepository;

    @InjectMocks
    private TransactionAnomalyServiceImpl transactionAnomalyService;

    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio();
        portfolio.setId(1L);
        portfolio.setPortfolioName("Growth Portfolio");
    }

    private Transaction transaction(long id, AssetType assetType, long assetId, BigDecimal quantity, BigDecimal price) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setPortfolio(portfolio);
        transaction.setAssetType(assetType);
        transaction.setAssetId(assetId);
        transaction.setTransactionType(TransactionType.BUY);
        transaction.setQuantity(quantity);
        transaction.setPrice(price);
        transaction.setTransactionDate(LocalDate.now().minusDays(id));
        return transaction;
    }

    @Nested
    @DisplayName("Portfolio validation")
    class PortfolioValidationTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException for an unknown portfolio")
        void shouldThrowWhenPortfolioMissing() {
            when(portfolioRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> transactionAnomalyService.detect(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should return no anomalies when there are no transactions")
        void shouldReturnEmptyWhenNoTransactions() {
            when(portfolioRepository.existsById(1L)).thenReturn(true);
            when(transactionRepository.findByPortfolioIdOrderByTransactionDateDescIdDesc(1L)).thenReturn(List.of());

            List<TransactionAnomalyResponse> result = transactionAnomalyService.detect(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Price deviation detection")
    class PriceDeviationTests {

        @Test
        @DisplayName("Should flag a transaction priced far away from the current stock price")
        void shouldFlagPriceDeviation() {
            Stock stock = new Stock();
            stock.setId(1L);
            stock.setPrice(new BigDecimal("100.00"));

            Transaction outlier = transaction(1L, AssetType.STOCK, 1L, new BigDecimal("10"), new BigDecimal("200.00"));

            when(portfolioRepository.existsById(1L)).thenReturn(true);
            when(transactionRepository.findByPortfolioIdOrderByTransactionDateDescIdDesc(1L)).thenReturn(List.of(outlier));
            when(stockRepository.findById(1L)).thenReturn(Optional.of(stock));

            List<TransactionAnomalyResponse> result = transactionAnomalyService.detect(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSeverity()).isEqualTo(AnomalySeverity.HIGH);
            assertThat(result.get(0).getReasons()).anyMatch(reason -> reason.contains("reference price"));
        }

        @Test
        @DisplayName("Should not flag a transaction priced close to the current stock price")
        void shouldNotFlagNormalPrice() {
            Stock stock = new Stock();
            stock.setId(1L);
            stock.setPrice(new BigDecimal("100.00"));

            Transaction normal = transaction(1L, AssetType.STOCK, 1L, new BigDecimal("10"), new BigDecimal("101.00"));

            when(portfolioRepository.existsById(1L)).thenReturn(true);
            when(transactionRepository.findByPortfolioIdOrderByTransactionDateDescIdDesc(1L)).thenReturn(List.of(normal));
            when(stockRepository.findById(1L)).thenReturn(Optional.of(stock));

            List<TransactionAnomalyResponse> result = transactionAnomalyService.detect(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Statistical outlier detection")
    class StatisticalOutlierTests {

        @Test
        @DisplayName("Should flag a moderately-sized outlier as MEDIUM (n=6)")
        void shouldFlagMediumOutlier() {
            CashAsset cashAsset = new CashAsset();
            cashAsset.setId(1L);
            cashAsset.setExchangeRate(new BigDecimal("1.00"));
            when(cashAssetRepository.findById(1L)).thenReturn(Optional.of(cashAsset));

            List<Transaction> transactions = new ArrayList<>();
            for (long i = 1; i <= 5; i++) {
                transactions.add(transaction(i, AssetType.CASH, 1L, new BigDecimal("10"), new BigDecimal("1.00")));
            }
            transactions.add(transaction(6L, AssetType.CASH, 1L, new BigDecimal("10000"), new BigDecimal("1.00")));

            when(portfolioRepository.existsById(1L)).thenReturn(true);
            when(transactionRepository.findByPortfolioIdOrderByTransactionDateDescIdDesc(1L)).thenReturn(transactions);

            List<TransactionAnomalyResponse> result = transactionAnomalyService.detect(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTransactionId()).isEqualTo(6L);
            assertThat(result.get(0).getSeverity()).isEqualTo(AnomalySeverity.MEDIUM);
        }

        @Test
        @DisplayName("Should flag a large outlier as HIGH (n=11)")
        void shouldFlagHighOutlier() {
            CashAsset cashAsset = new CashAsset();
            cashAsset.setId(1L);
            cashAsset.setExchangeRate(new BigDecimal("1.00"));
            when(cashAssetRepository.findById(1L)).thenReturn(Optional.of(cashAsset));

            List<Transaction> transactions = new ArrayList<>();
            for (long i = 1; i <= 10; i++) {
                transactions.add(transaction(i, AssetType.CASH, 1L, new BigDecimal("10"), new BigDecimal("1.00")));
            }
            transactions.add(transaction(11L, AssetType.CASH, 1L, new BigDecimal("1000000"), new BigDecimal("1.00")));

            when(portfolioRepository.existsById(1L)).thenReturn(true);
            when(transactionRepository.findByPortfolioIdOrderByTransactionDateDescIdDesc(1L)).thenReturn(transactions);

            List<TransactionAnomalyResponse> result = transactionAnomalyService.detect(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTransactionId()).isEqualTo(11L);
            assertThat(result.get(0).getSeverity()).isEqualTo(AnomalySeverity.HIGH);
        }

        @Test
        @DisplayName("Should skip the statistical signal when fewer than 5 transactions exist")
        void shouldSkipStatisticalSignalForSmallSample() {
            CashAsset cashAsset = new CashAsset();
            cashAsset.setId(1L);
            cashAsset.setExchangeRate(new BigDecimal("1.00"));
            when(cashAssetRepository.findById(1L)).thenReturn(Optional.of(cashAsset));

            List<Transaction> transactions = new ArrayList<>();
            for (long i = 1; i <= 3; i++) {
                transactions.add(transaction(i, AssetType.CASH, 1L, new BigDecimal("10"), new BigDecimal("1.00")));
            }
            transactions.add(transaction(4L, AssetType.CASH, 1L, new BigDecimal("10000"), new BigDecimal("1.00")));

            when(portfolioRepository.existsById(1L)).thenReturn(true);
            when(transactionRepository.findByPortfolioIdOrderByTransactionDateDescIdDesc(1L)).thenReturn(transactions);

            List<TransactionAnomalyResponse> result = transactionAnomalyService.detect(1L);

            assertThat(result).isEmpty();
        }
    }
}
