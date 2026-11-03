package com.spendmart.banking_service.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Component
public class BankAuthClient {

    private final RestClient restClient;

    public BankAuthClient(@Qualifier("bankAuthRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<String> getToken(String cognitoJwt, String provider) {
        try {
            TokenResponse response = restClient.get()
                    .uri("/api/v1/token/{provider}", provider)
                    .header("Authorization", "Bearer " + cognitoJwt)
                    .retrieve()
                    .body(TokenResponse.class);

            if (response == null || response.token() == null) {
                return Optional.empty();
            }
            return Optional.of(response.token());
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientResponseException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            String reason = e.getResponseBodyAsString();
            if (reason == null || reason.isBlank()) {
                reason = "bank-auth-service token lookup failed";
            }
            if (status == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, reason, e);
            }
            throw new ResponseStatusException(status, reason, e);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "bank-auth-service is unreachable", e);
        }
    }

    private record TokenResponse(@JsonProperty("token") String token) {}
}
