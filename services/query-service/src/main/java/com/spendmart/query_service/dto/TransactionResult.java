package com.spendmart.query_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

/**
 * DTO representing the result of a transaction query.
 * maps directly to NormalisedTransaction
 */
@Data
public class TransactionResult {

    private UUID id;
    private String userId;
    private String externalTxnId;
    private String provider;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime occurredAt;

}
