package com.spendmart.expense_service.repository;

import com.spendmart.schema.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    boolean existsByExpense_Id(UUID expenseId);

    List<Settlement> findByExpense_Id(UUID expenseId);

    Optional<Settlement> findByLinkIdAndExpense_Id(UUID linkId, UUID expenseId);

    void deleteByExpense_Id(UUID expenseId);
}
