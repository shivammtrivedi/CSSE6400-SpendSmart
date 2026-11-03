package com.spendmart.banking_service.adapter;

import com.spendmart.banking_service.dto.NormalisedTransaction;

import java.util.List;

public interface BankProviderAdapter {
    String providerKey();
    List<NormalisedTransaction> fetchTransactions(String token, String userId);
}
