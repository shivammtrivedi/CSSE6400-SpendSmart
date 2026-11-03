package com.spendmart.bank_auth_service.controller;

import com.spendmart.bank_auth_service.client.BankApiNodeJsClient;
import com.spendmart.bank_auth_service.client.ObpDirectLoginClient;
import com.spendmart.bank_auth_service.dto.BankApiLoginRequest;
import com.spendmart.bank_auth_service.dto.ObpLoginRequest;
import com.spendmart.bank_auth_service.service.TokenStore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/login")
@RequiredArgsConstructor
public class BankAuthController {

    private final ObpDirectLoginClient obpDirectLoginClient;
    private final BankApiNodeJsClient bankApiNodeJsClient;
    private final TokenStore tokenStore;

    @PostMapping("/obp")
    public ResponseEntity<Void> loginObp(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ObpLoginRequest credentials) {
        String userId = jwt.getSubject();
        String token = obpDirectLoginClient.login(credentials);
        tokenStore.put(userId, "obp", token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bankapi")
    public ResponseEntity<Void> loginBankApi(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BankApiLoginRequest credentials) {
        String userId = jwt.getSubject();
        String token = bankApiNodeJsClient.login(credentials);
        tokenStore.put(userId, "bankapi", token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }
}
