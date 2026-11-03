package com.spendmart.bank_auth_service.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spendmart.bank_auth_service.dto.BankApiLoginRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BankApiNodeJsClient {
    private static final Logger log = LoggerFactory.getLogger(BankApiNodeJsClient.class);

    private final RestClient bankApiRestClient;

    public BankApiNodeJsClient(@Qualifier("bankApiRestClient") RestClient bankApiRestClient) {
        this.bankApiRestClient = bankApiRestClient;
    }

    public String login(BankApiLoginRequest request) {
        BankApiAuthResponse response;
        try {
            response = bankApiRestClient.post()
                    .uri("/api/login")
                    .body(request)
                    .retrieve()
                    .body(BankApiAuthResponse.class);
        } catch (RestClientResponseException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            String reason = ex.getResponseBodyAsString();
            if (reason == null || reason.isBlank()) {
                reason = "Bank API login failed";
            }
            log.warn("bank-api-nodejs login failed: status={}, body={}",
                    ex.getStatusCode().value(), reason);
            if (status == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, reason, ex);
            }
            throw new ResponseStatusException(status, reason, ex);
        } catch (ResourceAccessException ex) {
            log.error("bank-api-nodejs unreachable while attempting login", ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "bank-api-nodejs is unreachable", ex);
        }

        if (response == null || response.token() == null) {
            throw new IllegalStateException("BankApiNodeJs returned no token");
        }
        return response.token();
    }

    private record BankApiAuthResponse(@JsonProperty("token") String token) {}
}
