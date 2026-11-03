package com.spendmart.query_service.client;

import java.util.List;

import com.spendmart.query_service.dto.TransactionResult;

/**
 * client interface for communicating with the Banking Service to fetch transaction data.
 * @param userId the ID of the user whose transactions are being fetched
 * @param jwkToken the JWT token for authentication with the Banking Service
 */
public interface BankingServiceClient {
    List<TransactionResult> getTransactions(String userId, String jwkToken);
}