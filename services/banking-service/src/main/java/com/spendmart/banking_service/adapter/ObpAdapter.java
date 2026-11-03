package com.spendmart.banking_service.adapter;

import com.spendmart.banking_service.client.ObpApiClient;
import com.spendmart.banking_service.dto.NormalisedTransaction;
import com.spendmart.banking_service.dto.ObpTransactionResponse;
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
@Order(1)
@RequiredArgsConstructor
public class ObpAdapter implements BankProviderAdapter {

    private final ObpApiClient obpApiClient;

    @Override
    public String providerKey() {
        return "obp";
    }

    @Override
    public List<NormalisedTransaction> fetchTransactions(String token, String userId) {
        return obpApiClient.getTransactions(token).stream()
                .map(obp -> toNormalised(obp, userId))
                .collect(Collectors.toList());
    }

    private NormalisedTransaction toNormalised(ObpTransactionResponse obp, String userId) {
        NormalisedTransaction t = new NormalisedTransaction();
        t.setId(UUID.randomUUID());
        t.setUserId(userId);
        t.setExternalTxnId(obp.getId());
        t.setProvider(providerKey());

        if (obp.getDetails() != null) {
            t.setOccurredAt(parseDateTime(obp.getDetails().getPostedDate()));

            if (obp.getDetails().getValue() != null) {
                String raw = obp.getDetails().getValue().getAmount();
                t.setAmount(raw != null ? new BigDecimal(raw).abs() : BigDecimal.ZERO);
                t.setCurrency(obp.getDetails().getValue().getCurrency());
            }
        }
        return t;
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
