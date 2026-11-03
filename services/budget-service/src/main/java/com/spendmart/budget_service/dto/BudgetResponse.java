package com.spendmart.budget_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetResponse(UUID id, String category, BigDecimal limitAmount, String period) {}
