package com.spendmart.budget_service.controller;

import com.spendmart.budget_service.dto.BudgetRequest;
import com.spendmart.budget_service.dto.BudgetResponse;
import com.spendmart.budget_service.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<BudgetResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BudgetRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.create(userId, req));
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(budgetService.getByUser(userId));
    }

    @PutMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID budgetId,
            @Valid @RequestBody BudgetRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(budgetService.update(userId, budgetId, req));
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID budgetId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        budgetService.delete(userId, budgetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }
}
