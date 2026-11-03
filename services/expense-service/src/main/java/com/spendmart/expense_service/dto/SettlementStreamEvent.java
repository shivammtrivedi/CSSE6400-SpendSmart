package com.spendmart.expense_service.dto;

import java.time.Instant;
import java.util.UUID;

public record SettlementStreamEvent(
        SettlementEventType type,
        UUID groupId,
        UUID expenseId,
        UUID settlementId,
        Long version,
        Instant timestamp
) implements GroupStreamEvent {
    @Override
    public String eventName() {
        return type.name();
    }
}
