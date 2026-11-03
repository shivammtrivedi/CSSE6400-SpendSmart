package com.spendmart.query_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Canonical transaction row used by expense-service settlement checks.
 */
@Entity
@Table(name = "transactions")
@Data
public class CanonicalTransaction {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "external_txn_id", nullable = false)
    private String externalTxnId;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
