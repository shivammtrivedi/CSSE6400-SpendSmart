package com.spendmart.banking_service.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spendmart.banking_service.dto.ObpTransactionResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ObpApiClient {

    private final RestClient restClient;

    @Value("${obp.api.version}")
    private String apiVersion;

    public ObpApiClient(@Qualifier("obpRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<ObpTransactionResponse> getTransactions(String token) {
        ObpTransactionsWrapper wrapper = restClient.get()
                .uri("/{version}/my/transactions", apiVersion)
                .header("Authorization", "DirectLogin token=" + token)
                .retrieve()
                .body(ObpTransactionsWrapper.class);

        return wrapper != null && wrapper.transactions() != null ? wrapper.transactions() : List.of();
    }

    private record ObpTransactionsWrapper(
            @JsonProperty("transactions") List<ObpTransactionResponse> transactions
    ) {}
}
