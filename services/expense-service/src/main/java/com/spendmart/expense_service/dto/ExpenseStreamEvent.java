package com.spendmart.expense_service.dto;

import java.time.Instant;
import java.util.UUID;

public record ExpenseStreamEvent(
        ExpenseEventType type,
        UUID groupId,
        UUID expenseId,
        Long version,
        Instant timestamp
) implements GroupStreamEvent {
    @Override
    public String eventName() {
        return type.name();
    }
}
