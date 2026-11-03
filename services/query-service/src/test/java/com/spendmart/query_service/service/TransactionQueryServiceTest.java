package com.spendmart.query_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spendmart.query_service.client.BankingServiceClient;
import com.spendmart.query_service.dto.TransactionFilter;
import com.spendmart.query_service.dto.TransactionResult;
//claude was used to update the tests to reflect the changes from using the banking stub +  updated fields 
// in NormalisedTransaction
@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceTest {

    @Mock
    private BankingServiceClient bankingServiceClient;

    private TransactionQueryService service;

    private static final String USER_ID = "user1";
    private static final String JWT = "fake-jwt";

    @BeforeEach
    void setUp() {
        service = new TransactionQueryService(bankingServiceClient);
    }

    private List<TransactionResult> createTestTransactions() {
        TransactionResult t1 = new TransactionResult();
        t1.setId(UUID.randomUUID());
        t1.setUserId(USER_ID);
        t1.setExternalTxnId("ext-001");
        t1.setProvider("obp");
        t1.setAmount(new BigDecimal("150.00"));
        t1.setCurrency("AUD");
        t1.setOccurredAt(LocalDateTime.of(2026, 5, 1, 9, 30));

        TransactionResult t2 = new TransactionResult();
        t2.setId(UUID.randomUUID());
        t2.setUserId(USER_ID);
        t2.setExternalTxnId("ext-002");
        t2.setProvider("obp");
        t2.setAmount(new BigDecimal("2500.00"));
        t2.setCurrency("AUD");
        t2.setOccurredAt(LocalDateTime.of(2026, 5, 2, 12, 0));

        TransactionResult t3 = new TransactionResult();
        t3.setId(UUID.randomUUID());
        t3.setUserId(USER_ID);
        t3.setExternalTxnId("ext-003");
        t3.setProvider("bankapi");
        t3.setAmount(new BigDecimal("45.50"));
        t3.setCurrency("USD");
        t3.setOccurredAt(LocalDateTime.of(2026, 5, 3, 15, 45));

        return List.of(t1, t2, t3);
    }

    @Test
    void shouldReturnAllTransactionsWhenNoFilter() {
        when(bankingServiceClient.getTransactions(USER_ID, JWT)).thenReturn(createTestTransactions());

        List<TransactionResult> results = service.queryTransactions(USER_ID, JWT, null);

        assertEquals(3, results.size());
        verify(bankingServiceClient).getTransactions(USER_ID, JWT);
    }

    @Test
    void shouldFilterByProvider() {
        when(bankingServiceClient.getTransactions(USER_ID, JWT)).thenReturn(createTestTransactions());

        TransactionFilter filter = new TransactionFilter();
        filter.setProvider("obp");

        List<TransactionResult> results = service.queryTransactions(USER_ID, JWT, filter);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(t -> "obp".equalsIgnoreCase(t.getProvider())));
    }

    @Test
    void shouldFilterByCurrencyAUD() {
        when(bankingServiceClient.getTransactions(USER_ID, JWT)).thenReturn(createTestTransactions());

        TransactionFilter filter = new TransactionFilter();
        filter.setCurrency("AUD");

        List<TransactionResult> results = service.queryTransactions(USER_ID, JWT, filter);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(t -> "AUD".equalsIgnoreCase(t.getCurrency())));
    }

    @Test
    void shouldFilterByCurrencyCaseInsensitive() {
        when(bankingServiceClient.getTransactions(USER_ID, JWT)).thenReturn(createTestTransactions());

        TransactionFilter filter = new TransactionFilter();
        filter.setCurrency("usd");

        List<TransactionResult> results = service.queryTransactions(USER_ID, JWT, filter);

        assertEquals(1, results.size());
        assertEquals("USD", results.get(0).getCurrency());
    }

    @Test
    void shouldFilterByDateRange() {
        when(bankingServiceClient.getTransactions(USER_ID, JWT)).thenReturn(createTestTransactions());

        TransactionFilter filter = new TransactionFilter();
        filter.setFromDate(java.time.LocalDate.of(2026, 5, 2));
        filter.setToDate(java.time.LocalDate.of(2026, 5, 3));

        List<TransactionResult> results = service.queryTransactions(USER_ID, JWT, filter);

        assertEquals(2, results.size());
    }

    @Test
    void shouldFilterByMinAmount() {
        when(bankingServiceClient.getTransactions(USER_ID, JWT)).thenReturn(createTestTransactions());

        TransactionFilter filter = new TransactionFilter();
        filter.setMinAmount(new BigDecimal("100"));

        List<TransactionResult> results = service.queryTransactions(USER_ID, JWT, filter);

        assertEquals(2, results.size());
    }

    @Test
    void shouldFilterByMaxAmount() {
        when(bankingServiceClient.getTransactions(USER_ID, JWT)).thenReturn(createTestTransactions());

        TransactionFilter filter = new TransactionFilter();
        filter.setMaxAmount(new BigDecimal("200"));

        List<TransactionResult> results = service.queryTransactions(USER_ID, JWT, filter);

        assertEquals(2, results.size()); // 150.00 and 45.50
    }

    @Test
    void shouldReturnEmptyWhenNoMatch() {
        when(bankingServiceClient.getTransactions(USER_ID, JWT)).thenReturn(createTestTransactions());

        TransactionFilter filter = new TransactionFilter();
        filter.setProvider("nonexistent");

        List<TransactionResult> results = service.queryTransactions(USER_ID, JWT, filter);

        assertTrue(results.isEmpty());
    }
}