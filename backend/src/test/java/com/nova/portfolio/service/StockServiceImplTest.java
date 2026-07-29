package com.nova.portfolio.service;

import com.nova.portfolio.dto.StockRequest;
import com.nova.portfolio.dto.StockResponse;
import com.nova.portfolio.exception.ResourceNotFoundException;
import com.nova.portfolio.mapper.StockMapper;
import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.model.Stock;
import com.nova.portfolio.repository.HoldingRepository;
import com.nova.portfolio.repository.StockRepository;
import com.nova.portfolio.repository.TransactionRepository;
import com.nova.portfolio.service.impl.StockServiceImpl;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockServiceImpl Tests")
class StockServiceImplTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private StockServiceImpl stockService;

    private StockRequest validStockRequest;
    private Stock mockStock;
    private StockResponse mockStockResponse;

    @BeforeEach
    void setUp() {
        validStockRequest = new StockRequest();
        validStockRequest.setSymbol("AAPL");
        validStockRequest.setName("Apple Inc.");
        validStockRequest.setSector("Technology");
        validStockRequest.setExchange("NASDAQ");
        validStockRequest.setPrice(new BigDecimal("150.00"));
        validStockRequest.setMarketCap(new BigDecimal("2500000000.00"));

        mockStock = new Stock();
        mockStock.setId(1L);
        mockStock.setSymbol("AAPL");
        mockStock.setName("Apple Inc.");
        mockStock.setSector("Technology");
        mockStock.setExchange("NASDAQ");
        mockStock.setPrice(new BigDecimal("150.00"));
        mockStock.setMarketCap(new BigDecimal("2500000000.00"));
        ReflectionTestUtils.setField(mockStock, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(mockStock, "updatedAt", LocalDateTime.now());

        mockStockResponse = new StockResponse();
        mockStockResponse.setId(1L);
        mockStockResponse.setSymbol("AAPL");
        mockStockResponse.setName("Apple Inc.");
        mockStockResponse.setSector("Technology");
        mockStockResponse.setExchange("NASDAQ");
        mockStockResponse.setPrice(new BigDecimal("150.00"));
        mockStockResponse.setMarketCap(new BigDecimal("2500000000.00"));
    }

    @Nested
    @DisplayName("Create Stock Tests")
    class CreateStockTests {

        @Test
        @DisplayName("Should successfully create a new stock")
        void shouldCreateNewStock() {
            // Given
            when(stockRepository.existsBySymbol("AAPL")).thenReturn(false);
            when(stockRepository.save(any(Stock.class))).thenReturn(mockStock);

            // When
            StockResponse result = stockService.create(validStockRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSymbol()).isEqualTo("AAPL");
            assertThat(result.getName()).isEqualTo("Apple Inc.");
            assertThat(result.getSector()).isEqualTo("Technology");
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("150.00"));

            verify(stockRepository).existsBySymbol("AAPL");
            verify(stockRepository).save(any(Stock.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when symbol already exists")
        void shouldThrowExceptionWhenSymbolExists() {
            // Given
            when(stockRepository.existsBySymbol("AAPL")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> stockService.create(validStockRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stock symbol already exists");

            verify(stockRepository).existsBySymbol("AAPL");
            verify(stockRepository, never()).save(any(Stock.class));
        }

        @Test
        @DisplayName("Should create stock with minimal fields")
        void shouldCreateStockWithMinimalFields() {
            // Given
            StockRequest minimalRequest = new StockRequest();
            minimalRequest.setSymbol("MSFT");
            minimalRequest.setPrice(new BigDecimal("300.00"));

            Stock savedStock = new Stock();
            savedStock.setId(2L);
            savedStock.setSymbol("MSFT");
            savedStock.setPrice(new BigDecimal("300.00"));
            ReflectionTestUtils.setField(savedStock, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(savedStock, "updatedAt", LocalDateTime.now());

            when(stockRepository.existsBySymbol("MSFT")).thenReturn(false);
            when(stockRepository.save(any(Stock.class))).thenReturn(savedStock);

            // When
            StockResponse result = stockService.create(minimalRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getSymbol()).isEqualTo("MSFT");
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("300.00"));
        }
    }

    @Nested
    @DisplayName("Find All Stocks Tests")
    class FindAllStocksTests {

        @Test
        @DisplayName("Should return all stocks")
        void shouldReturnAllStocks() {
            // Given
            Stock stock2 = new Stock();
            stock2.setId(2L);
            stock2.setSymbol("MSFT");
            stock2.setPrice(new BigDecimal("300.00"));
            ReflectionTestUtils.setField(stock2, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(stock2, "updatedAt", LocalDateTime.now());

            when(stockRepository.findAll()).thenReturn(List.of(mockStock, stock2));

            // When
            List<StockResponse> result = stockService.findAll();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
            assertThat(result.get(1).getSymbol()).isEqualTo("MSFT");

            verify(stockRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no stocks exist")
        void shouldReturnEmptyList() {
            // Given
            when(stockRepository.findAll()).thenReturn(List.of());

            // When
            List<StockResponse> result = stockService.findAll();

            // Then
            assertThat(result).isEmpty();

            verify(stockRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Find Stock by ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find stock by ID successfully")
        void shouldFindStockById() {
            // Given
            when(stockRepository.findById(1L)).thenReturn(Optional.of(mockStock));

            // When
            StockResponse result = stockService.findById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSymbol()).isEqualTo("AAPL");

            verify(stockRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when stock not found")
        void shouldThrowExceptionWhenStockNotFound() {
            // Given
            when(stockRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> stockService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Stock not found for id: 999");

            verify(stockRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("Update Stock Tests")
    class UpdateStockTests {

        @Test
        @DisplayName("Should update stock successfully")
        void shouldUpdateStockSuccessfully() {
            // Given
            StockRequest updateRequest = new StockRequest();
            updateRequest.setSymbol("AAPL");
            updateRequest.setName("Apple Computer Inc.");
            updateRequest.setPrice(new BigDecimal("155.00"));

            Stock updatedStock = new Stock();
            updatedStock.setId(1L);
            updatedStock.setSymbol("AAPL");
            updatedStock.setName("Apple Computer Inc.");
            updatedStock.setPrice(new BigDecimal("155.00"));
            ReflectionTestUtils.setField(updatedStock, "createdAt", LocalDateTime.now().minusDays(1));
            ReflectionTestUtils.setField(updatedStock, "updatedAt", LocalDateTime.now());

            when(stockRepository.findById(1L)).thenReturn(Optional.of(mockStock));
            when(stockRepository.existsBySymbol("AAPL")).thenReturn(true);
            when(stockRepository.save(any(Stock.class))).thenReturn(updatedStock);

            // When
            StockResponse result = stockService.update(1L, updateRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSymbol()).isEqualTo("AAPL");
            assertThat(result.getName()).isEqualTo("Apple Computer Inc.");
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("155.00"));

            verify(stockRepository).findById(1L);
            verify(stockRepository).existsBySymbol("AAPL");
            verify(stockRepository).save(any(Stock.class));
        }

        @Test
        @DisplayName("Should update stock with new symbol")
        void shouldUpdateStockWithNewSymbol() {
            // Given
            StockRequest updateRequest = new StockRequest();
            updateRequest.setSymbol("APPL");
            updateRequest.setPrice(new BigDecimal("155.00"));

            Stock updatedStock = new Stock();
            updatedStock.setId(1L);
            updatedStock.setSymbol("APPL");
            updatedStock.setPrice(new BigDecimal("155.00"));
            ReflectionTestUtils.setField(updatedStock, "createdAt", LocalDateTime.now().minusDays(1));
            ReflectionTestUtils.setField(updatedStock, "updatedAt", LocalDateTime.now());

            when(stockRepository.findById(1L)).thenReturn(Optional.of(mockStock));
            when(stockRepository.existsBySymbol("APPL")).thenReturn(false);
            when(stockRepository.save(any(Stock.class))).thenReturn(updatedStock);

            // When
            StockResponse result = stockService.update(1L, updateRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSymbol()).isEqualTo("APPL");

            verify(stockRepository).findById(1L);
            verify(stockRepository).existsBySymbol("APPL");
            verify(stockRepository).save(any(Stock.class));
        }

        @Test
        @DisplayName("Should throw exception when updating to duplicate symbol")
        void shouldThrowExceptionWhenDuplicateSymbol() {
            // Given
            StockRequest updateRequest = new StockRequest();
            updateRequest.setSymbol("MSFT");
            updateRequest.setPrice(new BigDecimal("300.00"));

            when(stockRepository.findById(1L)).thenReturn(Optional.of(mockStock));
            when(stockRepository.existsBySymbol("MSFT")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> stockService.update(1L, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stock symbol already exists");

            verify(stockRepository).findById(1L);
            verify(stockRepository).existsBySymbol("MSFT");
        }

        @Test
        @DisplayName("Should throw exception when stock not found during update")
        void shouldThrowExceptionWhenStockNotFoundForUpdate() {
            // Given
            when(stockRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> stockService.update(999L, validStockRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Stock not found for id: 999");

            verify(stockRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("Delete Stock Tests")
    class DeleteStockTests {

        @Test
        @DisplayName("Should delete stock successfully")
        void shouldDeleteStockSuccessfully() {
            // Given
            when(stockRepository.existsById(1L)).thenReturn(true);
            when(holdingRepository.existsByAssetTypeAndAssetId(AssetType.STOCK, 1L)).thenReturn(false);
            when(transactionRepository.existsByAssetTypeAndAssetId(AssetType.STOCK, 1L)).thenReturn(false);

            // When
            stockService.delete(1L);

            // Then
            verify(stockRepository).existsById(1L);
            verify(holdingRepository).existsByAssetTypeAndAssetId(AssetType.STOCK, 1L);
            verify(transactionRepository).existsByAssetTypeAndAssetId(AssetType.STOCK, 1L);
            verify(stockRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when stock not found during delete")
        void shouldThrowExceptionWhenStockNotFoundForDelete() {
            // Given
            when(stockRepository.existsById(999L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> stockService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Stock not found for id: 999");

            verify(stockRepository).existsById(999L);
            verify(stockRepository, never()).deleteById(999L);
        }

        @Test
        @DisplayName("Should throw exception when stock is referenced by holdings")
        void shouldThrowExceptionWhenReferencedByHoldings() {
            // Given
            when(stockRepository.existsById(1L)).thenReturn(true);
            when(holdingRepository.existsByAssetTypeAndAssetId(AssetType.STOCK, 1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> stockService.delete(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot delete stock: it is referenced by existing holdings");

            verify(stockRepository).existsById(1L);
            verify(holdingRepository).existsByAssetTypeAndAssetId(AssetType.STOCK, 1L);
            verify(stockRepository, never()).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when stock is referenced by transactions")
        void shouldThrowExceptionWhenReferencedByTransactions() {
            // Given
            when(stockRepository.existsById(1L)).thenReturn(true);
            when(holdingRepository.existsByAssetTypeAndAssetId(AssetType.STOCK, 1L)).thenReturn(false);
            when(transactionRepository.existsByAssetTypeAndAssetId(AssetType.STOCK, 1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> stockService.delete(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot delete stock: it is referenced by existing transactions");

            verify(stockRepository).existsById(1L);
            verify(holdingRepository).existsByAssetTypeAndAssetId(AssetType.STOCK, 1L);
            verify(transactionRepository).existsByAssetTypeAndAssetId(AssetType.STOCK, 1L);
            verify(stockRepository, never()).deleteById(1L);
        }
    }
}

