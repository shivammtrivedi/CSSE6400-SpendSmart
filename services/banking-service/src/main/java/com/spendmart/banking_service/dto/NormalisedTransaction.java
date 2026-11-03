package com.spendmart.banking_service.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class NormalisedTransaction {

    private UUID id;
    private String userId;
    private String externalTxnId;
    private String provider;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime occurredAt;
}
