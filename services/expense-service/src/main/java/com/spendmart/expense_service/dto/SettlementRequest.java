package com.spendmart.expense_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SettlementRequest {

    @NotNull
    private Long version;

    // required for POST only
    @NotBlank
    private String provider;

    @NotBlank
    private String transactionId;

    @NotNull
    @Positive
    private BigDecimal amount;
}
