package com.nova.portfolio.service;

import com.nova.portfolio.dto.AssetAllocationSliceResponse;
import com.nova.portfolio.dto.ForecastPointResponse;
import com.nova.portfolio.dto.PortfolioForecastResponse;
import com.nova.portfolio.dto.PortfolioSummaryResponse;
import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.service.impl.PortfolioForecastServiceImpl;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioForecastServiceImpl Tests")
class PortfolioForecastServiceImplTest {

    @Mock
    private PortfolioAnalyticsService portfolioAnalyticsService;

    @InjectMocks
    private PortfolioForecastServiceImpl portfolioForecastService;

    private PortfolioSummaryResponse allStockSummary;

    @BeforeEach
    void setUp() {
        allStockSummary = new PortfolioSummaryResponse();
        allStockSummary.setTotalValue(new BigDecimal("10000"));
        allStockSummary.setAllocation(List.of(slice(AssetType.STOCK, new BigDecimal("10000"))));
    }

    private AssetAllocationSliceResponse slice(AssetType assetType, BigDecimal value) {
        AssetAllocationSliceResponse slice = new AssetAllocationSliceResponse();
        slice.setLabel(assetType.name());
        slice.setValue(value);
        return slice;
    }

    @Nested
    @DisplayName("Forecast Tests")
    class ForecastTests {

        @Test
        @DisplayName("Should project 100% stock allocation using the stock return assumption")
        void shouldProjectAllStockAllocation() {
            when(portfolioAnalyticsService.getPortfolioSummary(1L)).thenReturn(allStockSummary);

            PortfolioForecastResponse result = portfolioForecastService.forecast(1L, 12);

            assertThat(result.getPoints()).hasSize(12);
            assertThat(result.getAssumedAnnualReturnPercent()).isEqualByComparingTo(new BigDecimal("7.00"));

            ForecastPointResponse lastPoint = result.getPoints().get(11);
            assertThat(lastPoint.getMonth()).isEqualTo(12);
            assertThat(lastPoint.getProjectedValue()).isEqualByComparingTo(new BigDecimal("10700.00"));
            assertThat(lastPoint.getLowerBound()).isLessThan(lastPoint.getProjectedValue());
            assertThat(lastPoint.getUpperBound()).isGreaterThan(lastPoint.getProjectedValue());
            assertThat(result.getDisclaimer()).isNotBlank();
        }

        @Test
        @DisplayName("Should blend return/volatility across a mixed stock+bond allocation")
        void shouldBlendMixedAllocation() {
            PortfolioSummaryResponse mixedSummary = new PortfolioSummaryResponse();
            mixedSummary.setTotalValue(new BigDecimal("10000"));
            mixedSummary.setAllocation(List.of(
                slice(AssetType.STOCK, new BigDecimal("5000")),
                slice(AssetType.BOND, new BigDecimal("5000"))
            ));
            when(portfolioAnalyticsService.getPortfolioSummary(2L)).thenReturn(mixedSummary);

            PortfolioForecastResponse result = portfolioForecastService.forecast(2L, 6);

            assertThat(result.getAssumedAnnualReturnPercent()).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(result.getAssumedAnnualVolatilityPercent()).isEqualByComparingTo(new BigDecimal("9.49"));
        }

        @Test
        @DisplayName("Should return a flat zero projection when the portfolio has no value")
        void shouldReturnZeroProjectionForEmptyPortfolio() {
            PortfolioSummaryResponse emptySummary = new PortfolioSummaryResponse();
            emptySummary.setTotalValue(BigDecimal.ZERO);
            emptySummary.setAllocation(List.of());
            when(portfolioAnalyticsService.getPortfolioSummary(3L)).thenReturn(emptySummary);

            PortfolioForecastResponse result = portfolioForecastService.forecast(3L, 3);

            assertThat(result.getPoints()).allSatisfy(point ->
                assertThat(point.getProjectedValue()).isEqualByComparingTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Should reject an out-of-range horizon")
        void shouldRejectOutOfRangeHorizon() {
            assertThatThrownBy(() -> portfolioForecastService.forecast(1L, 0))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> portfolioForecastService.forecast(1L, 61))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
