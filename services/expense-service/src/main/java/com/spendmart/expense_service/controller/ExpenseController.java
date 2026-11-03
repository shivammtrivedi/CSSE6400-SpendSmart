package com.spendmart.expense_service.controller;

import com.spendmart.expense_service.dto.ExpenseRequest;
import com.spendmart.expense_service.dto.ExpenseResponse;
import com.spendmart.expense_service.dto.VersionRequest;
import com.spendmart.expense_service.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expense")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ExpenseRequest request) {
        UUID userId = parseUuid(jwt.getSubject(), "userId");
        return ResponseEntity.ok(expenseService.create(userId, request));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<List<ExpenseResponse>> getByGroup(
            @PathVariable String groupId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID parsedGroupId = parseUuid(groupId, "groupId");
        UUID userId = parseUuid(jwt.getSubject(), "userId");
        return ResponseEntity.ok(expenseService.getByGroup(parsedGroupId, userId));
    }

    @GetMapping("/item/{expenseId}")
    public ResponseEntity<ExpenseResponse> getById(
            @PathVariable String expenseId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID parsedExpenseId = parseUuid(expenseId, "expenseId");
        UUID userId = parseUuid(jwt.getSubject(), "userId");
        return ResponseEntity.ok(expenseService.getById(parsedExpenseId, userId));
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> update(
            @PathVariable String expenseId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ExpenseRequest request) {
        UUID parsedExpenseId = parseUuid(expenseId, "expenseId");
        UUID userId = parseUuid(jwt.getSubject(), "userId");
        return ResponseEntity.ok(expenseService.update(parsedExpenseId, userId, request));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> delete(
            @PathVariable String expenseId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VersionRequest request) {
        UUID parsedExpenseId = parseUuid(expenseId, "expenseId");
        UUID userId = parseUuid(jwt.getSubject(), "userId");
        expenseService.delete(parsedExpenseId, userId, request.version());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }

    private UUID parseUuid(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Invalid UUID for " + fieldName);
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid UUID for " + fieldName);
        }
    }
}
