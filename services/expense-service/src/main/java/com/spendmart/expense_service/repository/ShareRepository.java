package com.spendmart.expense_service.repository;

import com.spendmart.schema.entity.Share;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShareRepository extends JpaRepository<Share, UUID> {

    List<Share> findByExpense_Id(UUID expenseId);

    Optional<Share> findByExpense_IdAndUser_Id(UUID expenseId, UUID userId);

    void deleteByExpense_Id(UUID expenseId);
}
