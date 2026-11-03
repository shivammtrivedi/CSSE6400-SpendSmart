package com.spendmart.expense_service.repository;

import com.spendmart.schema.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByGroup_Id(UUID groupId);

    List<Expense> findByIssuer_Id(UUID userId);
}
