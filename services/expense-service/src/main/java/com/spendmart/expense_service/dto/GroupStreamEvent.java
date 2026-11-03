package com.spendmart.expense_service.dto;

import java.time.Instant;
import java.util.UUID;

public interface GroupStreamEvent {
    UUID groupId();

    Instant timestamp();

    String eventName();
}
