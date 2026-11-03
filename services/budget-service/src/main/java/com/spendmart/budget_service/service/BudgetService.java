package com.spendmart.budget_service.service;

import com.spendmart.budget_service.dto.BudgetRequest;
import com.spendmart.budget_service.dto.BudgetResponse;
import com.spendmart.budget_service.entity.Budget;
import com.spendmart.budget_service.exception.ForbiddenException;
import com.spendmart.budget_service.repository.BudgetRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;

    @Transactional
    public BudgetResponse create(UUID userId, BudgetRequest req) {
        Budget budget = Budget.builder()
                .userId(userId)
                .category(req.getCategory())
                .limitAmount(req.getLimitAmount())
                .period(req.getPeriod())
                .build();
        return toResponse(budgetRepository.save(budget));
    }

    public List<BudgetResponse> getByUser(UUID userId) {
        return budgetRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BudgetResponse update(UUID userId, UUID budgetId, BudgetRequest req) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found: " + budgetId));
        if (!budget.getUserId().equals(userId)) {
            throw new ForbiddenException("Budget does not belong to the current user");
        }
        budget.setCategory(req.getCategory());
        budget.setLimitAmount(req.getLimitAmount());
        budget.setPeriod(req.getPeriod());
        return toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public void delete(UUID userId, UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found: " + budgetId));
        if (!budget.getUserId().equals(userId)) {
            throw new ForbiddenException("Budget does not belong to the current user");
        }
        budgetRepository.delete(budget);
    }

    private BudgetResponse toResponse(Budget b) {
        return new BudgetResponse(b.getId(), b.getCategory(), b.getLimitAmount(), b.getPeriod());
    }
}
