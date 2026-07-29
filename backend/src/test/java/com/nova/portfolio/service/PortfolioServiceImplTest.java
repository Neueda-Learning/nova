package com.nova.portfolio.service;

import com.nova.portfolio.dto.PortfolioRequest;
import com.nova.portfolio.dto.PortfolioResponse;
import com.nova.portfolio.exception.ResourceNotFoundException;
import com.nova.portfolio.mapper.PortfolioMapper;
import com.nova.portfolio.model.Portfolio;
import com.nova.portfolio.repository.HoldingRepository;
import com.nova.portfolio.repository.PortfolioRepository;
import com.nova.portfolio.repository.TransactionRepository;
import com.nova.portfolio.service.impl.PortfolioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
@DisplayName("PortfolioServiceImpl Tests")
class PortfolioServiceImplTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    private PortfolioRequest validPortfolioRequest;
    private Portfolio mockPortfolio;
    private PortfolioResponse mockPortfolioResponse;

    @BeforeEach
    void setUp() {
        validPortfolioRequest = new PortfolioRequest();
        validPortfolioRequest.setPortfolioName("Growth Portfolio");
        validPortfolioRequest.setDescription("A portfolio focused on growth stocks");

        mockPortfolio = new Portfolio();
        mockPortfolio.setId(1L);
        mockPortfolio.setPortfolioName("Growth Portfolio");
        mockPortfolio.setDescription("A portfolio focused on growth stocks");
        ReflectionTestUtils.setField(mockPortfolio, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(mockPortfolio, "updatedAt", LocalDateTime.now());

        mockPortfolioResponse = new PortfolioResponse();
        mockPortfolioResponse.setId(1L);
        mockPortfolioResponse.setPortfolioName("Growth Portfolio");
        mockPortfolioResponse.setDescription("A portfolio focused on growth stocks");
        mockPortfolioResponse.setCreatedAt(LocalDateTime.now());
        mockPortfolioResponse.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Create Portfolio Tests")
    class CreatePortfolioTests {

        @Test
        @DisplayName("Should successfully create a new portfolio")
        void shouldCreateNewPortfolio() {
            // Given
            when(portfolioRepository.existsByPortfolioName("Growth Portfolio")).thenReturn(false);
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(mockPortfolio);

            // When
            PortfolioResponse result = portfolioService.create(validPortfolioRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getPortfolioName()).isEqualTo("Growth Portfolio");
            assertThat(result.getDescription()).isEqualTo("A portfolio focused on growth stocks");

            verify(portfolioRepository).existsByPortfolioName("Growth Portfolio");
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when portfolio name already exists")
        void shouldThrowExceptionWhenNameExists() {
            // Given
            when(portfolioRepository.existsByPortfolioName("Growth Portfolio")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> portfolioService.create(validPortfolioRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Portfolio name already exists");

            verify(portfolioRepository).existsByPortfolioName("Growth Portfolio");
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should create portfolio with minimal fields")
        void shouldCreatePortfolioWithMinimalFields() {
            // Given
            PortfolioRequest minimalRequest = new PortfolioRequest();
            minimalRequest.setPortfolioName("Income Portfolio");

            Portfolio savedPortfolio = new Portfolio();
            savedPortfolio.setId(2L);
            savedPortfolio.setPortfolioName("Income Portfolio");
            ReflectionTestUtils.setField(savedPortfolio, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(savedPortfolio, "updatedAt", LocalDateTime.now());

            when(portfolioRepository.existsByPortfolioName("Income Portfolio")).thenReturn(false);
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(savedPortfolio);

            // When
            PortfolioResponse result = portfolioService.create(minimalRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getPortfolioName()).isEqualTo("Income Portfolio");
            assertThat(result.getDescription()).isNull();

            verify(portfolioRepository).existsByPortfolioName("Income Portfolio");
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should create portfolio with only portfolio name")
        void shouldCreatePortfolioWithOnlyName() {
            // Given
            PortfolioRequest request = new PortfolioRequest();
            request.setPortfolioName("Value Portfolio");
            request.setDescription(null);

            Portfolio savedPortfolio = new Portfolio();
            savedPortfolio.setId(3L);
            savedPortfolio.setPortfolioName("Value Portfolio");
            savedPortfolio.setDescription(null);
            ReflectionTestUtils.setField(savedPortfolio, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(savedPortfolio, "updatedAt", LocalDateTime.now());

            when(portfolioRepository.existsByPortfolioName("Value Portfolio")).thenReturn(false);
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(savedPortfolio);

            // When
            PortfolioResponse result = portfolioService.create(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPortfolioName()).isEqualTo("Value Portfolio");

            verify(portfolioRepository).existsByPortfolioName("Value Portfolio");
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should create multiple portfolios with different names")
        void shouldCreateMultiplePortfolios() {
            // Given
            PortfolioRequest request1 = new PortfolioRequest();
            request1.setPortfolioName("Portfolio 1");

            PortfolioRequest request2 = new PortfolioRequest();
            request2.setPortfolioName("Portfolio 2");

            Portfolio saved1 = new Portfolio();
            saved1.setId(1L);
            saved1.setPortfolioName("Portfolio 1");
            ReflectionTestUtils.setField(saved1, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved1, "updatedAt", LocalDateTime.now());

            Portfolio saved2 = new Portfolio();
            saved2.setId(2L);
            saved2.setPortfolioName("Portfolio 2");
            ReflectionTestUtils.setField(saved2, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved2, "updatedAt", LocalDateTime.now());

            when(portfolioRepository.existsByPortfolioName("Portfolio 1")).thenReturn(false);
            when(portfolioRepository.existsByPortfolioName("Portfolio 2")).thenReturn(false);
            when(portfolioRepository.save(any(Portfolio.class)))
                .thenReturn(saved1)
                .thenReturn(saved2);

            // When
            PortfolioResponse result1 = portfolioService.create(request1);
            PortfolioResponse result2 = portfolioService.create(request2);

            // Then
            assertThat(result1.getPortfolioName()).isEqualTo("Portfolio 1");
            assertThat(result2.getPortfolioName()).isEqualTo("Portfolio 2");
            assertThat(result1.getId()).isNotEqualTo(result2.getId());
        }
    }

    @Nested
    @DisplayName("Find All Portfolios Tests")
    class FindAllPortfoliosTests {

        @Test
        @DisplayName("Should return all portfolios")
        void shouldReturnAllPortfolios() {
            // Given
            Portfolio portfolio2 = new Portfolio();
            portfolio2.setId(2L);
            portfolio2.setPortfolioName("Income Portfolio");
            ReflectionTestUtils.setField(portfolio2, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(portfolio2, "updatedAt", LocalDateTime.now());

            when(portfolioRepository.findAll()).thenReturn(List.of(mockPortfolio, portfolio2));

            // When
            List<PortfolioResponse> result = portfolioService.findAll();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getPortfolioName()).isEqualTo("Growth Portfolio");
            assertThat(result.get(1).getPortfolioName()).isEqualTo("Income Portfolio");

            verify(portfolioRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no portfolios exist")
        void shouldReturnEmptyList() {
            // Given
            when(portfolioRepository.findAll()).thenReturn(List.of());

            // When
            List<PortfolioResponse> result = portfolioService.findAll();

            // Then
            assertThat(result).isEmpty();

            verify(portfolioRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Find Portfolio by ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find portfolio by ID successfully")
        void shouldFindPortfolioById() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(mockPortfolio));

            // When
            PortfolioResponse result = portfolioService.findById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getPortfolioName()).isEqualTo("Growth Portfolio");

            verify(portfolioRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when portfolio not found")
        void shouldThrowExceptionWhenPortfolioNotFound() {
            // Given
            when(portfolioRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> portfolioService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Portfolio not found for id: 999");

            verify(portfolioRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("Update Portfolio Tests")
    class UpdatePortfolioTests {

        @Test
        @DisplayName("Should update portfolio successfully")
        void shouldUpdatePortfolioSuccessfully() {
            // Given
            PortfolioRequest updateRequest = new PortfolioRequest();
            updateRequest.setPortfolioName("Growth Portfolio");
            updateRequest.setDescription("Updated description");

            Portfolio updatedPortfolio = new Portfolio();
            updatedPortfolio.setId(1L);
            updatedPortfolio.setPortfolioName("Growth Portfolio");
            updatedPortfolio.setDescription("Updated description");
            ReflectionTestUtils.setField(updatedPortfolio, "createdAt", LocalDateTime.now().minusDays(1));
            ReflectionTestUtils.setField(updatedPortfolio, "updatedAt", LocalDateTime.now());

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(mockPortfolio));
            when(portfolioRepository.existsByPortfolioName("Growth Portfolio")).thenReturn(true);
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(updatedPortfolio);

            // When
            PortfolioResponse result = portfolioService.update(1L, updateRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getPortfolioName()).isEqualTo("Growth Portfolio");
            assertThat(result.getDescription()).isEqualTo("Updated description");

            verify(portfolioRepository).findById(1L);
            verify(portfolioRepository).existsByPortfolioName("Growth Portfolio");
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when updating to duplicate name")
        void shouldThrowExceptionWhenDuplicateName() {
            // Given
            PortfolioRequest updateRequest = new PortfolioRequest();
            updateRequest.setPortfolioName("Income Portfolio");

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(mockPortfolio));
            when(portfolioRepository.existsByPortfolioName("Income Portfolio")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> portfolioService.update(1L, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Portfolio name already exists");

            verify(portfolioRepository).findById(1L);
            verify(portfolioRepository).existsByPortfolioName("Income Portfolio");
        }

        @Test
        @DisplayName("Should throw exception when portfolio not found during update")
        void shouldThrowExceptionWhenPortfolioNotFoundForUpdate() {
            // Given
            when(portfolioRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> portfolioService.update(999L, validPortfolioRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Portfolio not found for id: 999");

            verify(portfolioRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("Delete Portfolio Tests")
    class DeletePortfolioTests {

        @Test
        @DisplayName("Should delete portfolio successfully")
        void shouldDeletePortfolioSuccessfully() {
            // Given
            when(portfolioRepository.existsById(1L)).thenReturn(true);

            // When
            portfolioService.delete(1L);

            // Then
            verify(portfolioRepository).existsById(1L);
            verify(transactionRepository).deleteByPortfolioId(1L);
            verify(holdingRepository).deleteByPortfolioId(1L);
            verify(portfolioRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when portfolio not found during delete")
        void shouldThrowExceptionWhenPortfolioNotFoundForDelete() {
            // Given
            when(portfolioRepository.existsById(999L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> portfolioService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Portfolio not found for id: 999");

            verify(portfolioRepository).existsById(999L);
            verify(portfolioRepository, never()).deleteById(999L);
        }
    }
}

