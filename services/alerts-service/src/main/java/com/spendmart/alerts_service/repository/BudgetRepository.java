package com.spendmart.alerts_service.repository;

import com.spendmart.schema.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUser_Id(UUID userId);

    Optional<Budget> findByUser_IdAndCategoryAndPeriod(UUID userId, String category, String period);
}
