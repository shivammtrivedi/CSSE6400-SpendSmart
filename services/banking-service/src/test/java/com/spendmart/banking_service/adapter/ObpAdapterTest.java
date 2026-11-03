package com.spendmart.banking_service.adapter;

import com.spendmart.banking_service.client.ObpApiClient;
import com.spendmart.banking_service.dto.NormalisedTransaction;
import com.spendmart.banking_service.dto.ObpTransactionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObpAdapterTest {

    @Mock
    private ObpApiClient obpApiClient;

    @InjectMocks
    private ObpAdapter obpAdapter;

    private ObpTransactionResponse buildObpTransaction(String id, String amount, String currency,
                                                       String postedDate) {
        ObpTransactionResponse t = new ObpTransactionResponse();
        t.setId(id);

        ObpTransactionResponse.ObpTransactionDetails details = new ObpTransactionResponse.ObpTransactionDetails();
        details.setPostedDate(postedDate);

        ObpTransactionResponse.ObpValue value = new ObpTransactionResponse.ObpValue();
        value.setAmount(amount);
        value.setCurrency(currency);
        details.setValue(value);
        t.setDetails(details);

        return t;
    }

    @Test
    void providerKey_returnsObp() {
        assertThat(obpAdapter.providerKey()).isEqualTo("obp");
    }

    @Test
    void fetchTransactions_mapsSchemaFieldsCorrectly() {
        when(obpApiClient.getTransactions("obp-token")).thenReturn(List.of(
                buildObpTransaction("txn-1", "42.50", "AUD", "2024-03-15T10:00:00Z")
        ));

        List<NormalisedTransaction> result = obpAdapter.fetchTransactions("obp-token", "user-123");

        assertThat(result).hasSize(1);
        NormalisedTransaction t = result.get(0);
        assertThat(t.getId()).isNotNull();
        assertThat(t.getExternalTxnId()).isEqualTo("txn-1");
        assertThat(t.getUserId()).isEqualTo("user-123");
        assertThat(t.getProvider()).isEqualTo("obp");
        assertThat(t.getAmount()).isEqualByComparingTo("42.50");
        assertThat(t.getCurrency()).isEqualTo("AUD");
        assertThat(t.getOccurredAt()).hasToString("2024-03-15T10:00");
    }

    @Test
    void fetchTransactions_storesAbsoluteAmount_forDebit() {
        when(obpApiClient.getTransactions("obp-token")).thenReturn(List.of(
                buildObpTransaction("txn-1", "-5.50", "AUD", "2024-03-15T10:00:00Z")
        ));

        NormalisedTransaction t = obpAdapter.fetchTransactions("obp-token", "user-123").get(0);

        assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("5.50"));
    }

    @Test
    void fetchTransactions_returnsEmptyList_whenObpHasNoTransactions() {
        when(obpApiClient.getTransactions("obp-token")).thenReturn(List.of());

        assertThat(obpAdapter.fetchTransactions("obp-token", "user-123")).isEmpty();
    }

    @Test
    void fetchTransactions_parsesLocalDateFormat() {
        when(obpApiClient.getTransactions("obp-token")).thenReturn(List.of(
                buildObpTransaction("txn-1", "10.00", "AUD", "2024-03-15")
        ));

        NormalisedTransaction t = obpAdapter.fetchTransactions("obp-token", "user-123").get(0);

        assertThat(t.getOccurredAt()).isEqualTo("2024-03-15T00:00:00");
    }
}
