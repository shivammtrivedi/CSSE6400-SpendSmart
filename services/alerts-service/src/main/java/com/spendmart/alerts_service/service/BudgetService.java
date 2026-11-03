package com.spendmart.alerts_service.service;

import com.spendmart.alerts_service.dto.BudgetRequest;
import com.spendmart.alerts_service.repository.BudgetRepository;
import com.spendmart.schema.entity.Budget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final AlertService alertService;

    public Budget create(String userId, BudgetRequest request) {
        // TODO: map request to Budget entity, set userId, save
        return null;
    }

    public List<Budget> getByUser(String userId) {
        // TODO: return budgetRepository.findByUserId(userId)
        return null;
    }

    public Budget update(String budgetId, String userId, BudgetRequest request) {
        // TODO: find budget, verify owner, update fields, save
        return null;
    }

    public void delete(String budgetId, String userId) {
        // TODO: find budget, verify owner, delete
    }
}
