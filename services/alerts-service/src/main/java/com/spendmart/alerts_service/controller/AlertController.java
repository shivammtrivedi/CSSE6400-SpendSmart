package com.spendmart.alerts_service.controller;

import com.spendmart.alerts_service.dto.AlertResponse;
import com.spendmart.alerts_service.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<List<AlertResponse>> getAlerts(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        // TODO: call alertService.getAlerts(userId)
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread")
    public ResponseEntity<List<AlertResponse>> getUnreadAlerts(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        // TODO: call alertService.getUnreadAlerts(userId)
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{alertId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String alertId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        // TODO: call alertService.markAsRead(alertId, userId)
        return ResponseEntity.ok().build();
    }

    @PostMapping("/evaluate")
    public ResponseEntity<Void> evaluate() {
        // Called bi-weekly by EventBridge (no auth required — internal trigger only)
        // TODO: for each user with budgets, sum expenses in current period, create alerts for exceeded/warning thresholds
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }
}
