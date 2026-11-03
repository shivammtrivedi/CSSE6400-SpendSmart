package com.spendmart.expense_service.repository;

import com.spendmart.schema.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByExternalTxnIdAndOwner_IdAndProvider(String externalTxnId, UUID ownerId, String provider);
}
