package com.spendmart.alerts_service.controller;

import com.spendmart.alerts_service.dto.BudgetRequest;
import com.spendmart.alerts_service.service.BudgetService;
import com.spendmart.schema.entity.Budget;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<Budget> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BudgetRequest request) {
        String userId = jwt.getSubject();
        // TODO: call budgetService.create(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<Budget>> getMyBudgets(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        // TODO: call budgetService.getByUser(userId)
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{budgetId}")
    public ResponseEntity<Budget> update(
            @PathVariable String budgetId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BudgetRequest request) {
        String userId = jwt.getSubject();
        // TODO: call budgetService.update(budgetId, userId, request)
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> delete(
            @PathVariable String budgetId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        // TODO: call budgetService.delete(budgetId, userId)
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }
}
