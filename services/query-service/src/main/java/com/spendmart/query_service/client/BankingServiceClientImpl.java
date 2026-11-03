package com.spendmart.query_service.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.spendmart.query_service.dto.TransactionResult;
/**
 * Implementation of the BankingServiceClient interface that uses RestTemplate to communicate with the Banking Service
 * calls GET /api/v1/banking/transactions endpoint to fetch transactions for a user, 
 * passing the jwt token in the authorization header for authentication.
 */

@Component
public class BankingServiceClientImpl implements BankingServiceClient {
    private static final Logger log = LoggerFactory.getLogger(BankingServiceClientImpl.class);

    private final RestTemplate restTemplate;
    private final String bankingServiceUrl;

    public BankingServiceClientImpl(RestTemplate restTemplate,
                                    @Value("${banking.service.url}") String bankingServiceUrl) {
        this.restTemplate = restTemplate;
        this.bankingServiceUrl = bankingServiceUrl;
    }

    @Override
    public List<TransactionResult> getTransactions(String userId, String jwkToken) {
        String url = bankingServiceUrl + "/api/v1/banking/transactions";
        log.info("QueryService -> BankingService request start: userId={}, url={}", userId, url);

        HttpHeaders headers = new HttpHeaders(); // create new HttpHeaders instance
        headers.setBearerAuth(jwkToken); // set the authorization header with the jwt token
        // entity is used to wrap the headers for the request
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            List<TransactionResult> results = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<List<TransactionResult>>() {})
                    .getBody();
            int resultCount = results != null ? results.size() : 0;
            log.info("QueryService -> BankingService request success: userId={}, resultCount={}", userId, resultCount);
            return results;
        } catch (RestClientResponseException ex) {
            HttpStatus mapped = HttpStatus.resolve(ex.getStatusCode().value());
            String responseBody = ex.getResponseBodyAsString();
            log.warn(
                    "QueryService -> BankingService request failed: userId={}, status={}, body={}",
                    userId, ex.getStatusCode().value(), responseBody
            );
            throw new ResponseStatusException(
                    mapped != null ? mapped : HttpStatus.BAD_GATEWAY,
                    "Banking service call failed",
                    ex
            );
        } catch (ResourceAccessException ex) {
            log.error("QueryService -> BankingService unreachable: userId={}", userId, ex);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Banking service is unreachable",
                    ex
            );
        }
    }
}
