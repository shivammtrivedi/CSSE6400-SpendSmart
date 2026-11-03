package com.spendmart.query_service.service;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.spendmart.query_service.client.BankingServiceClient;
import com.spendmart.query_service.dto.TransactionFilter;
import com.spendmart.query_service.dto.TransactionResult;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class TransactionQueryService {
 
    private final BankingServiceClient bankingServiceClient;
 
    /**
     * @param userId   cognito subject — passed to the client for tracing
     * @param jwtToken raw jwt value — forwarded to banking-service for auth
     * @param filter   optional filter criteria, aligned with NormalisedTransaction fields
     */
    public List<TransactionResult> queryTransactions(String userId, String jwtToken, TransactionFilter filter) {
        List<TransactionResult> transactions = bankingServiceClient.getTransactions(userId, jwtToken);
 
        Stream<TransactionResult> stream = transactions.stream();
        // if no filter criteria provided, return all transactions
        if (filter == null) {
            return stream.toList();
        }
 
        // filter by provider (eg obp/bankapi) 
        if (filter.getProvider() != null) {

            stream = stream.filter(t -> filter.getProvider().equalsIgnoreCase(t.getProvider()));
        }
 
        // amount range 
        // min
        if (filter.getMinAmount() != null) {
            stream = stream.filter(t -> t.getAmount() != null
                    && t.getAmount().compareTo(filter.getMinAmount()) >= 0);
        }
        // max
        if (filter.getMaxAmount() != null) {
            stream = stream.filter(t -> t.getAmount() != null
                    && t.getAmount().compareTo(filter.getMaxAmount()) <= 0);
        }
 
        // date range, compare against the occurredAt date 
        // from date
        if (filter.getFromDate() != null) {
            stream = stream.filter(t -> t.getOccurredAt() != null
                    && !t.getOccurredAt().toLocalDate().isBefore(filter.getFromDate()));
        }


        // to date
        if (filter.getToDate() != null) {

            stream = stream.filter(t -> t.getOccurredAt() != null
                    && !t.getOccurredAt().toLocalDate().isAfter(filter.getToDate()));
        }

 
        // currency 
        if (filter.getCurrency() != null) {
            stream = stream.filter(t -> filter.getCurrency().equalsIgnoreCase(t.getCurrency()));
        }
        // collect the filtered stream back into a list and return
 
        return stream.toList();
    }
}