package com.spendmart.banking_service.controller;

import com.spendmart.banking_service.dto.NormalisedTransaction;
import com.spendmart.banking_service.service.BankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/banking")
@RequiredArgsConstructor
public class BankingController {

    private final BankingService bankingService;

    @GetMapping("/transactions")
    public ResponseEntity<List<NormalisedTransaction>> getTransactions(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(bankingService.getTransactions(userId, jwt.getTokenValue()));
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<NormalisedTransaction> getTransaction(
            @PathVariable String transactionId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(bankingService.getTransaction(transactionId, userId, jwt.getTokenValue()));
    }

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }
}
