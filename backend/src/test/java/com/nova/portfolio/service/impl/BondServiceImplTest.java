package com.nova.portfolio.service.impl;

import com.nova.portfolio.dto.BondRequest;
import com.nova.portfolio.dto.BondResponse;
import com.nova.portfolio.exception.ResourceNotFoundException;
import com.nova.portfolio.model.AssetType;
import com.nova.portfolio.model.Bond;
import com.nova.portfolio.repository.BondRepository;
import com.nova.portfolio.repository.HoldingRepository;
import com.nova.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BondServiceImplTest {

    @Mock
    private BondRepository bondRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BondServiceImpl bondService;

    @Test
    void createShouldSaveMappedBondAndReturnResponse() {
        BondRequest request = createBondRequest("US10Y");
        ArgumentCaptor<Bond> captor = ArgumentCaptor.forClass(Bond.class);

        when(bondRepository.save(any(Bond.class))).thenAnswer(invocation -> {
            Bond bond = invocation.getArgument(0);
            bond.setId(101L);
            return bond;
        });

        BondResponse response = bondService.create(request);

        verify(bondRepository).save(captor.capture());
        Bond saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("US10Y");
        assertThat(saved.getBondType()).isEqualTo("Government");
        assertThat(saved.getIssuer()).isEqualTo("US Treasury");
        assertThat(saved.getInterestRate()).isEqualByComparingTo("3.5000");
        assertThat(saved.getMaturityDate()).isEqualTo(LocalDate.now().plusYears(5));
        assertThat(saved.getCurrentPrice()).isEqualByComparingTo("99.5000");
        assertThat(saved.getRiskLevel()).isEqualTo("LOW");

        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getName()).isEqualTo("US10Y");
        assertThat(response.getCurrentPrice()).isEqualByComparingTo("99.5000");
    }

    @Test
    void findAllShouldMapAllBonds() {
        Bond first = createBondEntity(1L, "US10Y");
        Bond second = createBondEntity(2L, "CN5Y");
        when(bondRepository.findAll()).thenReturn(List.of(first, second));

        List<BondResponse> responses = bondService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(BondResponse::getId).containsExactly(1L, 2L);
        assertThat(responses).extracting(BondResponse::getName).containsExactly("US10Y", "CN5Y");
    }

    @Test
    void findByIdShouldReturnMappedBondWhenPresent() {
        Bond bond = createBondEntity(7L, "UST-BOND");
        when(bondRepository.findById(7L)).thenReturn(Optional.of(bond));

        BondResponse response = bondService.findById(7L);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getName()).isEqualTo("UST-BOND");
        assertThat(response.getIssuer()).isEqualTo("US Treasury");
    }

    @Test
    void findByIdShouldThrowWhenBondMissing() {
        when(bondRepository.findById(88L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bondService.findById(88L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Bond not found for id: 88");
    }

    @Test
    void updateShouldApplyRequestFieldsAndReturnResponse() {
        Bond existing = createBondEntity(5L, "US10Y");
        BondRequest request = createBondRequest("US20Y");
        request.setIssuer("Treasury Department");
        request.setRiskLevel("MEDIUM");
        request.setCurrentPrice(new BigDecimal("101.2500"));

        when(bondRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(bondRepository.save(existing)).thenReturn(existing);

        BondResponse response = bondService.update(5L, request);

        verify(bondRepository).save(existing);
        assertThat(existing.getName()).isEqualTo("US20Y");
        assertThat(existing.getIssuer()).isEqualTo("Treasury Department");
        assertThat(existing.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(existing.getCurrentPrice()).isEqualByComparingTo("101.2500");
        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("US20Y");
        assertThat(response.getRiskLevel()).isEqualTo("MEDIUM");
    }

    @Test
    void updateShouldThrowWhenBondMissing() {
        when(bondRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bondService.update(404L, createBondRequest("Missing")))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Bond not found for id: 404");
    }

    @Test
    void deleteShouldThrowWhenBondMissing() {
        when(bondRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> bondService.delete(404L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Bond not found for id: 404");

        verify(bondRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteShouldThrowWhenBondReferencedByHolding() {
        when(bondRepository.existsById(9L)).thenReturn(true);
        when(holdingRepository.existsByAssetTypeAndAssetId(AssetType.BOND, 9L)).thenReturn(true);

        assertThatThrownBy(() -> bondService.delete(9L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot delete bond: it is referenced by existing holdings");

        verify(transactionRepository, never()).existsByAssetTypeAndAssetId(any(), any(Long.class));
        verify(bondRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteShouldThrowWhenBondReferencedByTransaction() {
        when(bondRepository.existsById(10L)).thenReturn(true);
        when(holdingRepository.existsByAssetTypeAndAssetId(AssetType.BOND, 10L)).thenReturn(false);
        when(transactionRepository.existsByAssetTypeAndAssetId(AssetType.BOND, 10L)).thenReturn(true);

        assertThatThrownBy(() -> bondService.delete(10L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot delete bond: it is referenced by existing transactions");

        verify(bondRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteShouldRemoveBondWhenUnreferenced() {
        when(bondRepository.existsById(11L)).thenReturn(true);
        when(holdingRepository.existsByAssetTypeAndAssetId(AssetType.BOND, 11L)).thenReturn(false);
        when(transactionRepository.existsByAssetTypeAndAssetId(AssetType.BOND, 11L)).thenReturn(false);

        bondService.delete(11L);

        verify(bondRepository).deleteById(11L);
    }

    private BondRequest createBondRequest(String name) {
        BondRequest request = new BondRequest();
        request.setName(name);
        request.setBondType("Government");
        request.setIssuer("US Treasury");
        request.setInterestRate(new BigDecimal("3.5000"));
        request.setMaturityDate(LocalDate.now().plusYears(5));
        request.setCurrentPrice(new BigDecimal("99.5000"));
        request.setRiskLevel("LOW");
        return request;
    }

    private Bond createBondEntity(Long id, String name) {
        Bond bond = new Bond();
        bond.setId(id);
        bond.setName(name);
        bond.setBondType("Government");
        bond.setIssuer("US Treasury");
        bond.setInterestRate(new BigDecimal("3.5000"));
        bond.setMaturityDate(LocalDate.now().plusYears(5));
        bond.setCurrentPrice(new BigDecimal("99.5000"));
        bond.setRiskLevel("LOW");
        return bond;
    }
}
