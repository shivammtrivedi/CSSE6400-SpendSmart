package com.spendmart.expense_service.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ExpenseResponse {

    private String id;
    private String groupId;
    private String userId;
    private BigDecimal amount;
    private Long version;
    private List<ExpenseRequest.SplitEntry> split;
}
