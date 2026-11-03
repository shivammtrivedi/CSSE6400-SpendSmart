package com.spendmart.banking_service.service;

import com.spendmart.banking_service.adapter.BankProviderAdapter;
import com.spendmart.banking_service.client.BankAuthClient;
import com.spendmart.banking_service.dto.NormalisedTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BankingService {

    private final List<BankProviderAdapter> adapters;
    private final BankAuthClient bankAuthClient;

    public List<NormalisedTransaction> getTransactions(String userId, String cognitoJwt) {
        for (BankProviderAdapter adapter : adapters) {
            Optional<String> token = bankAuthClient.getToken(cognitoJwt, adapter.providerKey());
            if (token.isPresent()) {
                return adapter.fetchTransactions(token.get(), userId);
            }
        }
        return List.of();
    }

    public NormalisedTransaction getTransaction(String transactionId, String userId, String cognitoJwt) {
        return getTransactions(userId, cognitoJwt).stream()
                .filter(t -> transactionId.equals(t.getExternalTxnId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transaction not found: " + transactionId));
    }
}
