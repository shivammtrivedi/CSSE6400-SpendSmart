package com.spendmart.query_service.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.spendmart.query_service.dto.TransactionFilter;
import com.spendmart.query_service.dto.TransactionResult;
import com.spendmart.query_service.model.CanonicalTransaction;
import com.spendmart.query_service.repository.CanonicalTransactionRepository;
import com.spendmart.query_service.service.TransactionQueryService;
import com.spendmart.query_service.validation.SearchRequestValidator;

import org.springframework.web.bind.annotation.CrossOrigin;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/query")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QueryController {
    private static final Logger log = LoggerFactory.getLogger(QueryController.class);

    private final TransactionQueryService transactionQueryService;
    private final SearchRequestValidator searchRequestValidator;
    private final CanonicalTransactionRepository canonicalTransactionRepository;

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResult>> queryTransactions(
            @AuthenticationPrincipal Jwt jwt,
            @ModelAttribute TransactionFilter filters) {
        Jwt principal = requireJwt(jwt);
        String userId = principal.getSubject();
        String token  = principal.getTokenValue();
        log.info("Query request received: userId={}, provider={}, fromDate={}, toDate={}, minAmount={}, maxAmount={}, currency={}",
                userId, filters.getProvider(), filters.getFromDate(), filters.getToDate(),
                filters.getMinAmount(), filters.getMaxAmount(), filters.getCurrency());

        List<String> validationErrors = searchRequestValidator.validate(filters);
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", validationErrors));
        }

        List<TransactionResult> results = transactionQueryService.queryTransactions(userId, token, filters);
        log.info("Query request completed: userId={}, resultCount={}", userId, results != null ? results.size() : 0);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/transactions/persist")
    public ResponseEntity<Void> persistTransaction(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody TransactionResult transaction) {
        String userId = requireJwt(jwt).getSubject();
        java.util.UUID ownerId = parseUserId(userId);
        validatePersistRequest(transaction);
        log.info("Persist request received: userId={}, externalTxnId={}, provider={}",
                userId, transaction.getExternalTxnId(), transaction.getProvider());

        CanonicalTransaction entity = canonicalTransactionRepository
                .findByExternalTxnId(transaction.getExternalTxnId())
                .orElseGet(CanonicalTransaction::new);

        if (entity.getId() != null && (!ownerId.equals(entity.getOwnerId())
                || !transaction.getProvider().equals(entity.getProvider()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction already exists for another owner/provider");
        }

        if (entity.getId() == null) {
            entity.setId(java.util.UUID.randomUUID());
        }
        entity.setOwnerId(ownerId);
        entity.setExternalTxnId(transaction.getExternalTxnId());
        entity.setProvider(transaction.getProvider());
        entity.setAmount(transaction.getAmount());
        entity.setCurrency(transaction.getCurrency());
        entity.setOccurredAt(transaction.getOccurredAt() != null ? transaction.getOccurredAt() : LocalDateTime.now());

        canonicalTransactionRepository.save(entity);
        log.info("Persist request completed: userId={}, externalTxnId={}", userId, transaction.getExternalTxnId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }

    private Jwt requireJwt(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication principal");
        }
        return jwt;
    }

    private java.util.UUID parseUserId(String userId) {
        try {
            return java.util.UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Authenticated userId is not a valid UUID", ex);
        }
    }

    private void validatePersistRequest(TransactionResult transaction) {
        if (transaction == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction payload is required");
        }
        if (transaction.getExternalTxnId() == null || transaction.getExternalTxnId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "externalTxnId is required");
        }
        if (transaction.getProvider() == null || transaction.getProvider().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "provider is required");
        }
        if (transaction.getAmount() == null || transaction.getAmount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be greater than zero");
        }
        if (transaction.getCurrency() == null || transaction.getCurrency().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currency is required");
        }
    }
}
