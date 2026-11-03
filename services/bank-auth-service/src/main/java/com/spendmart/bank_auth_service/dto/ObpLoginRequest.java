package com.spendmart.bank_auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ObpLoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank @JsonProperty("consumer_key") String consumerKey
) {}
