package com.spendmart.expense_service.controller;

import com.spendmart.expense_service.dto.SettlementRequest;
import com.spendmart.expense_service.dto.SettlementResponse;
import com.spendmart.expense_service.dto.VersionRequest;
import com.spendmart.expense_service.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expense/{expenseId}/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public ResponseEntity<List<SettlementResponse>> getSettlements(
            @PathVariable String expenseId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID parsedExpenseId = parseUuid(expenseId, "expenseId");
        UUID userId = parseUuid(jwt.getSubject(), "userId");
        return ResponseEntity.ok(settlementService.getByExpense(parsedExpenseId, userId));
    }

    @PostMapping
    public ResponseEntity<SettlementResponse> createSettlement(
            @PathVariable String expenseId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SettlementRequest request) {
        UUID parsedExpenseId = parseUuid(expenseId, "expenseId");
        UUID userId = parseUuid(jwt.getSubject(), "userId");
        return ResponseEntity.ok(settlementService.create(parsedExpenseId, userId, request));
    }

    @DeleteMapping("/{linkId}")
    public ResponseEntity<Void> deleteSettlement(
            @PathVariable String expenseId,
            @PathVariable String linkId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VersionRequest request) {
        UUID parsedExpenseId = parseUuid(expenseId, "expenseId");
        UUID parsedLinkId = parseUuid(linkId, "linkId");
        UUID userId = parseUuid(jwt.getSubject(), "userId");
        settlementService.delete(parsedExpenseId, parsedLinkId, userId, request.version());
        return ResponseEntity.noContent().build();
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
