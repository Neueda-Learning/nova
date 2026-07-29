package com.nova.portfolio.service.impl;

import com.nova.portfolio.dto.CashAssetRequest;
import com.nova.portfolio.dto.CashAssetResponse;
import com.nova.portfolio.exception.ResourceNotFoundException;
import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.model.CashAsset;
import com.nova.portfolio.repository.CashAssetRepository;
import com.nova.portfolio.repository.HoldingRepository;
import com.nova.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashAssetServiceImplTest {

    @Mock
    private CashAssetRepository cashAssetRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CashAssetServiceImpl cashAssetService;

    @Test
    void createShouldSaveMappedCashAssetWhenCurrencyIsUnique() {
        CashAssetRequest request = createCashAssetRequest("USD");
        ArgumentCaptor<CashAsset> captor = ArgumentCaptor.forClass(CashAsset.class);

        when(cashAssetRepository.existsByCurrency("USD")).thenReturn(false);
        when(cashAssetRepository.save(any(CashAsset.class))).thenAnswer(invocation -> {
            CashAsset asset = invocation.getArgument(0);
            asset.setId(201L);
            return asset;
        });

        CashAssetResponse response = cashAssetService.create(request);

        verify(cashAssetRepository).save(captor.capture());
        CashAsset saved = captor.getValue();
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getExchangeRate()).isEqualByComparingTo("1.000000");

        assertThat(response.getId()).isEqualTo(201L);
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getExchangeRate()).isEqualByComparingTo("1.000000");
    }

    @Test
    void createShouldThrowWhenCurrencyAlreadyExists() {
        CashAssetRequest request = createCashAssetRequest("USD");
        when(cashAssetRepository.existsByCurrency("USD")).thenReturn(true);

        assertThatThrownBy(() -> cashAssetService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cash currency already exists");

        verify(cashAssetRepository, never()).save(any(CashAsset.class));
    }

    @Test
    void findAllShouldMapAllCashAssets() {
        CashAsset usd = createCashAssetEntity(1L, "USD");
        CashAsset eur = createCashAssetEntity(2L, "EUR");
        when(cashAssetRepository.findAll()).thenReturn(List.of(usd, eur));

        List<CashAssetResponse> responses = cashAssetService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CashAssetResponse::getCurrency).containsExactly("USD", "EUR");
    }

    @Test
    void findByIdShouldReturnMappedCashAssetWhenPresent() {
        CashAsset asset = createCashAssetEntity(9L, "JPY");
        when(cashAssetRepository.findById(9L)).thenReturn(Optional.of(asset));

        CashAssetResponse response = cashAssetService.findById(9L);

        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getCurrency()).isEqualTo("JPY");
        assertThat(response.getExchangeRate()).isEqualByComparingTo("1.000000");
    }

    @Test
    void findByIdShouldThrowWhenCashAssetMissing() {
        when(cashAssetRepository.findById(88L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cashAssetService.findById(88L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Cash asset not found for id: 88");
    }

    @Test
    void updateShouldAllowSameCurrencyAndPersistChanges() {
        CashAsset existing = createCashAssetEntity(5L, "USD");
        CashAssetRequest request = createCashAssetRequest("USD");
        request.setExchangeRate(new BigDecimal("1.010000"));

        when(cashAssetRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(cashAssetRepository.existsByCurrency("USD")).thenReturn(true);
        when(cashAssetRepository.save(existing)).thenReturn(existing);

        CashAssetResponse response = cashAssetService.update(5L, request);

        verify(cashAssetRepository).save(existing);
        assertThat(existing.getCurrency()).isEqualTo("USD");
        assertThat(existing.getExchangeRate()).isEqualByComparingTo("1.010000");
        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getExchangeRate()).isEqualByComparingTo("1.010000");
    }

    @Test
    void updateShouldThrowWhenCashAssetMissing() {
        when(cashAssetRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cashAssetService.update(404L, createCashAssetRequest("USD")))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Cash asset not found for id: 404");
    }

    @Test
    void updateShouldThrowWhenChangingToDuplicateCurrency() {
        CashAsset existing = createCashAssetEntity(7L, "USD");
        CashAssetRequest request = createCashAssetRequest("EUR");

        when(cashAssetRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(cashAssetRepository.existsByCurrency("EUR")).thenReturn(true);

        assertThatThrownBy(() -> cashAssetService.update(7L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cash currency already exists");

        verify(cashAssetRepository, never()).save(any(CashAsset.class));
    }

    @Test
    void deleteShouldThrowWhenCashAssetMissing() {
        when(cashAssetRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> cashAssetService.delete(404L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Cash asset not found for id: 404");

        verify(cashAssetRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteShouldThrowWhenCashAssetReferencedByHolding() {
        when(cashAssetRepository.existsById(10L)).thenReturn(true);
        when(holdingRepository.existsByAssetTypeAndAssetId(AssetType.CASH, 10L)).thenReturn(true);

        assertThatThrownBy(() -> cashAssetService.delete(10L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot delete cash asset: it is referenced by existing holdings");

        verify(transactionRepository, never()).existsByAssetTypeAndAssetId(any(), any(Long.class));
        verify(cashAssetRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteShouldThrowWhenCashAssetReferencedByTransaction() {
        when(cashAssetRepository.existsById(11L)).thenReturn(true);
        when(holdingRepository.existsByAssetTypeAndAssetId(AssetType.CASH, 11L)).thenReturn(false);
        when(transactionRepository.existsByAssetTypeAndAssetId(AssetType.CASH, 11L)).thenReturn(true);

        assertThatThrownBy(() -> cashAssetService.delete(11L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot delete cash asset: it is referenced by existing transactions");

        verify(cashAssetRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteShouldRemoveCashAssetWhenUnreferenced() {
        when(cashAssetRepository.existsById(12L)).thenReturn(true);
        when(holdingRepository.existsByAssetTypeAndAssetId(AssetType.CASH, 12L)).thenReturn(false);
        when(transactionRepository.existsByAssetTypeAndAssetId(AssetType.CASH, 12L)).thenReturn(false);

        cashAssetService.delete(12L);

        verify(cashAssetRepository).deleteById(12L);
    }

    private CashAssetRequest createCashAssetRequest(String currency) {
        CashAssetRequest request = new CashAssetRequest();
        request.setCurrency(currency);
        request.setExchangeRate(new BigDecimal("1.000000"));
        return request;
    }

    private CashAsset createCashAssetEntity(Long id, String currency) {
        CashAsset asset = new CashAsset();
        asset.setId(id);
        asset.setCurrency(currency);
        asset.setExchangeRate(new BigDecimal("1.000000"));
        return asset;
    }
}
