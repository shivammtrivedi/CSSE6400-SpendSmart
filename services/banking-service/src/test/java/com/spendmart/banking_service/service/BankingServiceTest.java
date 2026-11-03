package com.spendmart.banking_service.service;

import com.spendmart.banking_service.adapter.BankProviderAdapter;
import com.spendmart.banking_service.client.BankAuthClient;
import com.spendmart.banking_service.dto.NormalisedTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankingServiceTest {

    @Mock
    private BankProviderAdapter obpAdapter;

    @Mock
    private BankProviderAdapter bankApiAdapter;

    @Mock
    private BankAuthClient bankAuthClient;

    private BankingService bankingService;

    @BeforeEach
    void setUp() {
        when(obpAdapter.providerKey()).thenReturn("obp");
        bankingService = new BankingService(List.of(obpAdapter, bankApiAdapter), bankAuthClient);
    }

    private NormalisedTransaction tx(String externalTxnId) {
        NormalisedTransaction t = new NormalisedTransaction();
        t.setId(UUID.randomUUID());
        t.setExternalTxnId(externalTxnId);
        t.setUserId("user-123");
        t.setProvider("obp");
        return t;
    }

    @Test
    void getTransactions_usesFirstAdapterWithToken() {
        when(bankAuthClient.getToken("jwt", "obp")).thenReturn(Optional.of("obp-tok"));
        when(obpAdapter.fetchTransactions("obp-tok", "user-123")).thenReturn(List.of(tx("txn-1")));

        List<NormalisedTransaction> result = bankingService.getTransactions("user-123", "jwt");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExternalTxnId()).isEqualTo("txn-1");
    }

    @Test
    void getTransactions_fallsBackToNextAdapter_whenFirstHasNoToken() {
        when(bankApiAdapter.providerKey()).thenReturn("bankapi");
        when(bankAuthClient.getToken("jwt", "obp")).thenReturn(Optional.empty());
        when(bankAuthClient.getToken("jwt", "bankapi")).thenReturn(Optional.of("ba-tok"));
        when(bankApiAdapter.fetchTransactions("ba-tok", "user-123")).thenReturn(List.of(tx("txn-2")));

        List<NormalisedTransaction> result = bankingService.getTransactions("user-123", "jwt");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExternalTxnId()).isEqualTo("txn-2");
    }

    @Test
    void getTransactions_returnsEmptyList_whenNoBankConnected() {
        when(bankApiAdapter.providerKey()).thenReturn("bankapi");
        when(bankAuthClient.getToken("jwt", "obp")).thenReturn(Optional.empty());
        when(bankAuthClient.getToken("jwt", "bankapi")).thenReturn(Optional.empty());

        assertThat(bankingService.getTransactions("user-123", "jwt")).isEmpty();
    }

    @Test
    void getTransaction_returnsMatchingTransaction() {
        when(bankAuthClient.getToken("jwt", "obp")).thenReturn(Optional.of("obp-tok"));
        when(obpAdapter.fetchTransactions("obp-tok", "user-123"))
                .thenReturn(List.of(tx("txn-1"), tx("txn-2")));

        NormalisedTransaction result = bankingService.getTransaction("txn-2", "user-123", "jwt");

        assertThat(result.getExternalTxnId()).isEqualTo("txn-2");
    }

    @Test
    void getTransaction_throws404_whenNotFound() {
        when(bankAuthClient.getToken("jwt", "obp")).thenReturn(Optional.of("obp-tok"));
        when(obpAdapter.fetchTransactions("obp-tok", "user-123")).thenReturn(List.of(tx("txn-1")));

        assertThatThrownBy(() -> bankingService.getTransaction("nonexistent", "user-123", "jwt"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
