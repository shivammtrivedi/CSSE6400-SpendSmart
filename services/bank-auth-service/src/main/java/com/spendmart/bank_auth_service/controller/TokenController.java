package com.spendmart.bank_auth_service.controller;

import com.spendmart.bank_auth_service.dto.TokenResponse;
import com.spendmart.bank_auth_service.service.TokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/token")
@RequiredArgsConstructor
public class TokenController {

    private final TokenStore tokenStore;

    @GetMapping("/{provider}")
    public ResponseEntity<TokenResponse> getToken(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String provider) {
        String userId = jwt.getSubject();
        String token = tokenStore.get(userId, provider)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No token found for provider '" + provider + "' — call POST /api/v1/login/" + provider + " first"));
        return ResponseEntity.ok(new TokenResponse(token));
    }
}
