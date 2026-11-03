package com.spendmart.expense_service.service;

import com.spendmart.expense_service.dto.SettlementRequest;
import com.spendmart.expense_service.dto.SettlementResponse;
import com.spendmart.expense_service.exception.ConflictException;
import com.spendmart.expense_service.exception.ForbiddenException;
import com.spendmart.expense_service.repository.ExpenseRepository;
import com.spendmart.expense_service.repository.SettlementRepository;
import com.spendmart.expense_service.repository.ShareRepository;
import com.spendmart.expense_service.repository.TransactionRepository;
import com.spendmart.schema.entity.Expense;
import com.spendmart.schema.entity.Settlement;
import com.spendmart.schema.entity.Share;
import com.spendmart.schema.entity.Transaction;
import com.spendmart.schema.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final EntityManager entityManager;
    private final ExpenseRepository expenseRepository;
    private final ShareRepository shareRepository;
    private final SettlementRepository settlementRepository;
    private final TransactionRepository transactionRepository;
    private final ExpenseEventPublisher expenseEventPublisher;

    @Transactional(readOnly = true)
    public List<SettlementResponse> getByExpense(UUID expenseId, UUID requestingUserId) {
        // Ensure expense exists.
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));

        // Ensure user is included on this expense.
        assertIncludedOnExpense(expenseId, requestingUserId);

        // Return all settlements for expenseId.
        return settlementRepository.findByExpense_Id(expense.getId()).stream()
                .map(this::toSettlementResponse)
                .toList();
    }

    @Transactional
    public SettlementResponse create(UUID expenseId, UUID userId, SettlementRequest request) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));

        Share userShare = assertIncludedOnExpense(expenseId, userId);

        assertVersionMatches(request.getVersion(), expense.getVersion());

        Transaction transaction = transactionRepository
                .findByExternalTxnIdAndOwner_IdAndProvider(request.getTransactionId(), userId, request.getProvider())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transaction not found for provider '" + request.getProvider() + "'"));

        Settlement settlement = Settlement.builder()
                .expense(expense)
                .user(entityManager.getReference(User.class, userId))
                .transaction(transaction)
                .share(userShare)
                .amount(request.getAmount())
                .build();
        Settlement saved = settlementRepository.save(settlement);
        expenseEventPublisher.settlementCreated(saved);

        return toSettlementResponse(saved);
    }

    @Transactional
    public void delete(UUID expenseId, UUID linkId, UUID userId, Long version) {
        Settlement settlement = settlementRepository.findByLinkIdAndExpense_Id(linkId, expenseId)
                .orElseThrow(() -> new EntityNotFoundException("Settlement not found"));

        if (!settlement.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Caller does not own this settlement");
        }

        assertVersionMatches(version, settlement.getVersion());

        settlementRepository.delete(settlement);
        expenseEventPublisher.settlementDeleted(settlement);
    }

    private Share assertIncludedOnExpense(UUID expenseId, UUID userId) {
        return shareRepository.findByExpense_IdAndUser_Id(expenseId, userId)
                .orElseThrow(() -> new ForbiddenException("Caller is not included on this expense"));
    }

    private void assertVersionMatches(Long requestVersion, Long currentVersion) {
        if (requestVersion == null) {
            throw new IllegalArgumentException("version is required");
        }
        if (!requestVersion.equals(currentVersion)) {
            throw new ConflictException("Version mismatch");
        }
    }

    private SettlementResponse toSettlementResponse(Settlement settlement) {
        SettlementResponse response = new SettlementResponse();
        response.setLinkId(settlement.getLinkId().toString());
        response.setExpenseId(settlement.getExpense().getId().toString());
        response.setUserId(settlement.getUser().getId().toString());
        response.setProvider(settlement.getTransaction().getProvider());
        response.setTransactionId(settlement.getTransaction().getExternalTxnId());
        response.setAmount(settlement.getAmount());
        response.setVersion(settlement.getVersion());
        return response;
    }
}
