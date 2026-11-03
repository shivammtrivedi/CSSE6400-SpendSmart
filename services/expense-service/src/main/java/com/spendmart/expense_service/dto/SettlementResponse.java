package com.spendmart.expense_service.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SettlementResponse {

    private String linkId;
    private String expenseId;
    private String userId;
    private String provider;
    private String transactionId;
    private BigDecimal amount;
    private Long version;
}
