package com.spendmart.expense_service.controller;

import com.spendmart.expense_service.service.ExpenseEventStreamService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expense/events")
public class ExpenseEventsController {

    private final ExpenseEventStreamService expenseEventStreamService;

    public ExpenseEventsController(ExpenseEventStreamService expenseEventStreamService) {
        this.expenseEventStreamService = expenseEventStreamService;
    }

    // Adds a stream to the set of listeners scoped to that group
    @GetMapping(value = "/{groupId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamGroupEvents(
            @PathVariable String groupId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID parsedGroupId = parseUuid(groupId, "groupId");
        UUID userId = parseUuid(jwt.getSubject(), "userId");
        return ResponseEntity.ok(expenseEventStreamService.subscribe(parsedGroupId, userId));
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
