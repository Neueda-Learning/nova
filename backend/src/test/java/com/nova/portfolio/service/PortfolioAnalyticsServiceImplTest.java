package com.nova.portfolio.service;

import com.nova.portfolio.dto.PortfolioDashboardResponse;
import com.nova.portfolio.dto.PortfolioSummaryResponse;
import com.nova.portfolio.exception.ResourceNotFoundException;
import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.model.Bond;
import com.nova.portfolio.model.CashAsset;
import com.nova.portfolio.model.Holding;
import com.nova.portfolio.model.Portfolio;
import com.nova.portfolio.model.Stock;
import com.nova.portfolio.repository.BondRepository;
import com.nova.portfolio.repository.CashAssetRepository;
import com.nova.portfolio.repository.HoldingRepository;
import com.nova.portfolio.repository.PortfolioRepository;
import com.nova.portfolio.repository.StockRepository;
import com.nova.portfolio.service.impl.PortfolioAnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioAnalyticsServiceImpl Tests - My Home Dashboard")
class PortfolioAnalyticsServiceImplTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private BondRepository bondRepository;

    @Mock
    private CashAssetRepository cashAssetRepository;

    @InjectMocks
    private PortfolioAnalyticsServiceImpl portfolioAnalyticsService;

    private Portfolio portfolio1;
    private Stock stock1;
    private Bond bond1;
    private CashAsset cashAsset1;
    private Holding holding1;

    @BeforeEach
    void setUp() {
        portfolio1 = new Portfolio();
        portfolio1.setId(1L);
        portfolio1.setPortfolioName("Growth Portfolio");
        portfolio1.setDescription("Growth focused");
        ReflectionTestUtils.setField(portfolio1, "createdAt", LocalDateTime.now().minusDays(30));
        ReflectionTestUtils.setField(portfolio1, "updatedAt", LocalDateTime.now());

        stock1 = new Stock();
        stock1.setId(1L);
        stock1.setSymbol("AAPL");
        stock1.setName("Apple Inc.");
        stock1.setPrice(new BigDecimal("150.00"));
        ReflectionTestUtils.setField(stock1, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(stock1, "updatedAt", LocalDateTime.now());

        bond1 = new Bond();
        bond1.setId(1L);
        bond1.setName("US Treasury Bond");
        bond1.setIssuer("US Government");
        bond1.setCurrentPrice(new BigDecimal("100.00"));
        ReflectionTestUtils.setField(bond1, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(bond1, "updatedAt", LocalDateTime.now());

        cashAsset1 = new CashAsset();
        cashAsset1.setId(1L);
        cashAsset1.setCurrency("USD");
        cashAsset1.setExchangeRate(new BigDecimal("1.000000"));

        holding1 = new Holding();
        holding1.setId(1L);
        holding1.setPortfolio(portfolio1);
        holding1.setAssetType(AssetType.STOCK);
        holding1.setAssetId(1L);
        holding1.setQuantity(new BigDecimal("10.0000"));
        holding1.setAverageCost(new BigDecimal("140.0000"));
        ReflectionTestUtils.setField(holding1, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(holding1, "updatedAt", LocalDateTime.now());
    }

    @Nested
    @DisplayName("Get Dashboard Summary Tests (My Home)")
    class GetDashboardSummaryTests {

        @Test
        @DisplayName("Should get dashboard summary with single portfolio")
        void shouldGetDashboardSummaryWithSinglePortfolio() {
            // Given
            when(portfolioRepository.findAll()).thenReturn(List.of(portfolio1));
            when(holdingRepository.findAll()).thenReturn(List.of(holding1));
            when(stockRepository.findAllById(any())).thenReturn(List.of(stock1));
            when(bondRepository.findAllById(any())).thenReturn(List.of());
            when(cashAssetRepository.findAllById(any())).thenReturn(List.of());

            // When
            PortfolioDashboardResponse result = portfolioAnalyticsService.getDashboardSummary();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPortfolios()).hasSize(1);
            assertThat(result.getPortfolios().get(0).getPortfolioName()).isEqualTo("Growth Portfolio");
            assertThat(result.getGlobalTotal()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(result.getAllocation()).isNotEmpty();

            verify(portfolioRepository).findAll();
            verify(holdingRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty dashboard when no portfolios exist")
        void shouldReturnEmptyDashboardWhenNoPortfolios() {
            // Given
            when(portfolioRepository.findAll()).thenReturn(List.of());
            when(holdingRepository.findAll()).thenReturn(List.of());

            // When
            PortfolioDashboardResponse result = portfolioAnalyticsService.getDashboardSummary();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPortfolios()).isEmpty();
            assertThat(result.getAllocation()).isEmpty();
            assertThat(result.getGlobalTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should calculate stock allocation correctly")
        void shouldCalculateStockAllocationCorrectly() {
            // Given
            when(portfolioRepository.findAll()).thenReturn(List.of(portfolio1));
            when(holdingRepository.findAll()).thenReturn(List.of(holding1));
            when(stockRepository.findAllById(any())).thenReturn(List.of(stock1));
            when(bondRepository.findAllById(any())).thenReturn(List.of());
            when(cashAssetRepository.findAllById(any())).thenReturn(List.of());

            // When
            PortfolioDashboardResponse result = portfolioAnalyticsService.getDashboardSummary();

            // Then
            assertThat(result.getAllocation()).hasSize(1);
            assertThat(result.getAllocation().get(0).getLabel()).isEqualTo("STOCK");
            assertThat(result.getAllocation().get(0).getValue()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(result.getAllocation().get(0).getPercent()).isEqualByComparingTo(new BigDecimal("100.0"));
        }
    }

    @Nested
    @DisplayName("Get Portfolio Summary Tests")
    class GetPortfolioSummaryTests {

        @Test
        @DisplayName("Should get portfolio summary successfully")
        void shouldGetPortfolioSummarySuccessfully() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio1));
            when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(holding1));
            when(stockRepository.findAllById(any())).thenReturn(List.of(stock1));
            when(bondRepository.findAllById(any())).thenReturn(List.of());
            when(cashAssetRepository.findAllById(any())).thenReturn(List.of());

            // When
            PortfolioSummaryResponse result = portfolioAnalyticsService.getPortfolioSummary(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getPortfolioName()).isEqualTo("Growth Portfolio");
            assertThat(result.getHoldingCount()).isEqualTo(1);
            assertThat(result.getHoldings()).hasSize(1);
            assertThat(result.getTotalValue()).isEqualByComparingTo(new BigDecimal("1500.00"));

            verify(portfolioRepository).findById(1L);
            verify(holdingRepository).findByPortfolioId(1L);
        }

        @Test
        @DisplayName("Should throw exception when portfolio not found")
        void shouldThrowExceptionWhenPortfolioNotFound() {
            // Given
            when(portfolioRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> portfolioAnalyticsService.getPortfolioSummary(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Portfolio not found for id: 999");

            verify(portfolioRepository).findById(999L);
        }

        @Test
        @DisplayName("Should include asset label in holdings")
        void shouldIncludeAssetLabelInHoldings() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio1));
            when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(holding1));
            when(stockRepository.findAllById(any())).thenReturn(List.of(stock1));
            when(bondRepository.findAllById(any())).thenReturn(List.of());
            when(cashAssetRepository.findAllById(any())).thenReturn(List.of());

            // When
            PortfolioSummaryResponse result = portfolioAnalyticsService.getPortfolioSummary(1L);

            // Then
            assertThat(result.getHoldings().get(0).getAssetLabel()).isEqualTo("AAPL - Apple Inc.");
            assertThat(result.getHoldings().get(0).getQuantity()).isEqualByComparingTo(new BigDecimal("10.0000"));
            assertThat(result.getHoldings().get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("Should calculate market value correctly")
        void shouldCalculateMarketValueCorrectly() {
            // Given: quantity = 10, price = 150
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio1));
            when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(holding1));
            when(stockRepository.findAllById(any())).thenReturn(List.of(stock1));
            when(bondRepository.findAllById(any())).thenReturn(List.of());
            when(cashAssetRepository.findAllById(any())).thenReturn(List.of());

            // When
            PortfolioSummaryResponse result = portfolioAnalyticsService.getPortfolioSummary(1L);

            // Then
            // market value = 10 * 150 = 1500
            assertThat(result.getHoldings().get(0).getMarketValue()).isEqualByComparingTo(new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("Should calculate unrealized PnL correctly")
        void shouldCalculateUnrealizedPnlCorrectly() {
            // Given: quantity = 10, average cost = 140, current price = 150
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio1));
            when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(holding1));
            when(stockRepository.findAllById(any())).thenReturn(List.of(stock1));
            when(bondRepository.findAllById(any())).thenReturn(List.of());
            when(cashAssetRepository.findAllById(any())).thenReturn(List.of());

            // When
            PortfolioSummaryResponse result = portfolioAnalyticsService.getPortfolioSummary(1L);

            // Then
            // unrealized PnL = 1500 - (140 * 10) = 1500 - 1400 = 100
            assertThat(result.getHoldings().get(0).getUnrealizedPnl()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Should handle portfolio with no holdings")
        void shouldHandlePortfolioWithNoHoldings() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio1));
            when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of());
            when(stockRepository.findAllById(any())).thenReturn(List.of());
            when(bondRepository.findAllById(any())).thenReturn(List.of());
            when(cashAssetRepository.findAllById(any())).thenReturn(List.of());

            // When
            PortfolioSummaryResponse result = portfolioAnalyticsService.getPortfolioSummary(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getHoldingCount()).isZero();
            assertThat(result.getHoldings()).isEmpty();
            assertThat(result.getTotalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should calculate has PnL data flag correctly")
        void shouldCalculateHasPnlDataFlag() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio1));
            when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(holding1));
            when(stockRepository.findAllById(any())).thenReturn(List.of(stock1));
            when(bondRepository.findAllById(any())).thenReturn(List.of());
            when(cashAssetRepository.findAllById(any())).thenReturn(List.of());

            // When
            PortfolioSummaryResponse result = portfolioAnalyticsService.getPortfolioSummary(1L);

            // Then
            assertThat(result.isHasPnlData()).isTrue();
        }
    }

    @Nested
    @DisplayName("Asset Resolution Tests")
    class AssetResolutionTests {

        @Test
        @DisplayName("Should resolve stock asset correctly")
        void shouldResolveStockAssetCorrectly() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio1));
            when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(holding1));
            when(stockRepository.findAllById(any())).thenReturn(List.of(stock1));
            when(bondRepository.findAllById(any())).thenReturn(List.of());
            when(cashAssetRepository.findAllById(any())).thenReturn(List.of());

            // When
            PortfolioSummaryResponse result = portfolioAnalyticsService.getPortfolioSummary(1L);

            // Then
            assertThat(result.getHoldings().get(0).getAssetLabel()).isEqualTo("AAPL - Apple Inc.");
            assertThat(result.getHoldings().get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("Should resolve missing stock as unknown asset")
        void shouldResolveMissingStockAsUnknownAsset() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio1));
            when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(holding1));
            when(stockRepository.findAllById(any())).thenReturn(List.of());
            when(bondRepository.findAllById(any())).thenReturn(List.of());
            when(cashAssetRepository.findAllById(any())).thenReturn(List.of());

            // When
            PortfolioSummaryResponse result = portfolioAnalyticsService.getPortfolioSummary(1L);

            // Then
            assertThat(result.getHoldings().get(0).getAssetLabel()).isEqualTo("#1");
            assertThat(result.getHoldings().get(0).getUnitPrice()).isNull();
        }

        @Test
        @DisplayName("Should resolve bond asset correctly")
        void shouldResolveBondAssetCorrectly() {
            // Given
            Holding bondHolding = new Holding();
            bondHolding.setId(2L);
            bondHolding.setPortfolio(portfolio1);
            bondHolding.setAssetType(AssetType.BOND);
            bondHolding.setAssetId(1L);
            bondHolding.setQuantity(new BigDecimal("5.0000"));
            bondHolding.setAverageCost(new BigDecimal("99.0000"));
            ReflectionTestUtils.setField(bondHolding, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(bondHolding, "updatedAt", LocalDateTime.now());

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio1));
            when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(bondHolding));
            when(stockRepository.findAllById(any())).thenReturn(List.of());
            when(bondRepository.findAllById(any())).thenReturn(List.of(bond1));
            when(cashAssetRepository.findAllById(any())).thenReturn(List.of());

            // When
            PortfolioSummaryResponse result = portfolioAnalyticsService.getPortfolioSummary(1L);

            // Then
            assertThat(result.getHoldings().get(0).getAssetLabel()).isEqualTo("US Treasury Bond (US Government)");
            assertThat(result.getHoldings().get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Should resolve cash asset correctly")
        void shouldResolveCashAssetCorrectly() {
            // Given
            Holding cashHolding = new Holding();
            cashHolding.setId(3L);
            cashHolding.setPortfolio(portfolio1);
            cashHolding.setAssetType(AssetType.CASH);
            cashHolding.setAssetId(1L);
            cashHolding.setQuantity(new BigDecimal("1000.0000"));
            cashHolding.setAverageCost(new BigDecimal("1.0000"));
            ReflectionTestUtils.setField(cashHolding, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(cashHolding, "updatedAt", LocalDateTime.now());

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio1));
            when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(cashHolding));
            when(stockRepository.findAllById(any())).thenReturn(List.of());
            when(bondRepository.findAllById(any())).thenReturn(List.of());
            when(cashAssetRepository.findAllById(any())).thenReturn(List.of(cashAsset1));

            // When
            PortfolioSummaryResponse result = portfolioAnalyticsService.getPortfolioSummary(1L);

            // Then
            assertThat(result.getHoldings().get(0).getAssetLabel()).isEqualTo("USD");
            assertThat(result.getHoldings().get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("1.000000"));
        }
    }
}

