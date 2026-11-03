package com.spendmart.query_service.repository;

import com.spendmart.query_service.model.CanonicalTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CanonicalTransactionRepository extends JpaRepository<CanonicalTransaction, UUID> {
    Optional<CanonicalTransaction> findByExternalTxnId(String externalTxnId);
}
