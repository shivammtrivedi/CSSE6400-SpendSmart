package com.spendmart.expense_service.dto;

import jakarta.validation.constraints.NotNull;

public record VersionRequest(@NotNull Long version) {}
