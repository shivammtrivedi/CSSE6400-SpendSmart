package com.spendmart.expense_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ExpenseRequest {

    @NotBlank
    private String groupId;

    @NotNull
    @Positive
    private BigDecimal amount;

    // version is required for PUT and DELETE; omitted on POST
    private Long version;

    private List<SplitEntry> split;

    @Data
    public static class SplitEntry {
        @NotBlank
        private String userId;

        @NotNull
        @Positive
        private BigDecimal amount;
    }
}
