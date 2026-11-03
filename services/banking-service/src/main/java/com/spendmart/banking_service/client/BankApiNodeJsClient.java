package com.spendmart.banking_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendmart.banking_service.dto.BankApiTransaction;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Component
public class BankApiNodeJsClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public BankApiNodeJsClient(@Qualifier("bankApiRestClient") RestClient restClient,
                               ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public long getAccountNumber(String token) {
        String userId = extractUserId(token);
        BankApiUser user;
        try {
            user = restClient.get()
                    .uri("/api/user/{id}", userId)
                    .retrieve()
                    .body(BankApiUser.class);
        } catch (RestClientResponseException ex) {
            throw mapDownstreamError(ex, "Bank API user lookup failed");
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "bank-api-nodejs is unreachable", ex);
        }

        if (user == null || user.accountNumber() == null) {
            throw new IllegalStateException("BankApiNodeJs returned no accountNumber for user " + userId);
        }
        return user.accountNumber();
    }

    public List<BankApiTransaction> getTransactions(long accountNumber) {
        BankApiTransaction[] transactions;
        try {
            transactions = restClient.get()
                    .uri("/api/transaction/{accountNumber}", accountNumber)
                    .retrieve()
                    .body(BankApiTransaction[].class);
        } catch (RestClientResponseException ex) {
            throw mapDownstreamError(ex, "Bank API transactions lookup failed");
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "bank-api-nodejs is unreachable", ex);
        }

        return transactions != null ? Arrays.asList(transactions) : List.of();
    }

    private ResponseStatusException mapDownstreamError(RestClientResponseException ex, String fallbackReason) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        String reason = ex.getResponseBodyAsString();
        if (reason == null || reason.isBlank()) {
            reason = fallbackReason;
        }
        if (status == null) {
            return new ResponseStatusException(HttpStatus.BAD_GATEWAY, reason, ex);
        }
        return new ResponseStatusException(status, reason, ex);
    }

    private String extractUserId(String token) {
        try {
            String payload = token.split("\\.")[1];
            byte[] bytes = Base64.getUrlDecoder().decode(addPadding(payload));
            JwtPayload parsed = objectMapper.readValue(bytes, JwtPayload.class);
            return parsed.userId();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode BankApiNodeJs JWT", e);
        }
    }

    private String addPadding(String base64) {
        return base64 + "=".repeat((4 - base64.length() % 4) % 4);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JwtPayload(@JsonProperty("user_id") String userId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BankApiUser(@JsonProperty("accountNumber") Long accountNumber) {}
}
