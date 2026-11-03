package com.spendmart.banking_service.adapter;

import com.spendmart.banking_service.client.BankApiNodeJsClient;
import com.spendmart.banking_service.dto.BankApiTransaction;
import com.spendmart.banking_service.dto.NormalisedTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Order(2)
@RequiredArgsConstructor
public class BankApiAdapter implements BankProviderAdapter {

    private final BankApiNodeJsClient bankApiNodeJsClient;

    @Override
    public String providerKey() {
        return "bankapi";
    }

    @Override
    public List<NormalisedTransaction> fetchTransactions(String token, String userId) {
        long accountNumber = bankApiNodeJsClient.getAccountNumber(token);
        return bankApiNodeJsClient.getTransactions(accountNumber).stream()
                .map(t -> toNormalised(t, userId))
                .collect(Collectors.toList());
    }

    private NormalisedTransaction toNormalised(BankApiTransaction t, String userId) {
        NormalisedTransaction n = new NormalisedTransaction();
        n.setId(UUID.randomUUID());
        n.setUserId(userId);
        n.setExternalTxnId(t.getId());
        n.setProvider(providerKey());
        n.setAmount(t.getTransactionAmount() != null
                ? BigDecimal.valueOf(t.getTransactionAmount()).abs()
                : BigDecimal.ZERO);
        n.setCurrency("NGN");
        n.setOccurredAt(parseDateTime(t.getTransactionTime()));
        return n;
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return OffsetDateTime.parse(raw).toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDate.parse(raw).atStartOfDay();
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
