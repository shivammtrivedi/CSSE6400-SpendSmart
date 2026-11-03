package com.spendmart.bank_auth_service.dto;

import jakarta.validation.constraints.NotBlank;

public record BankApiLoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {}
