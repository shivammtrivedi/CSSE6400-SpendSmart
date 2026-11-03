package com.spendmart.expense_service.service;

import com.spendmart.expense_service.dto.ExpenseRequest;
import com.spendmart.expense_service.dto.ExpenseResponse;
import com.spendmart.expense_service.repository.ExpenseRepository;
import com.spendmart.expense_service.repository.GroupMemberRepository;
import com.spendmart.expense_service.repository.SettlementRepository;
import com.spendmart.expense_service.repository.ShareRepository;
import com.spendmart.expense_service.exception.ConflictException;
import com.spendmart.expense_service.exception.ForbiddenException;
import com.spendmart.schema.entity.Expense;
import com.spendmart.schema.entity.Group;
import com.spendmart.schema.entity.Share;
import com.spendmart.schema.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    // Transactions used for atomicity, optimistic concurrency for synchronization

    private final EntityManager entityManager;
    private final ExpenseRepository expenseRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ShareRepository shareRepository;
    private final SettlementRepository settlementRepository;
    private final ExpenseEventPublisher expenseEventPublisher;

    /**
     * Creates an expense and its corresponding share rows after validating request invariants.
     */
    @Transactional
    public ExpenseResponse create(UUID userId, ExpenseRequest request) {

        // Preconditions:

        // Parse and authorize caller against target group.
        UUID groupId = parseUuid(request.getGroupId(), "groupId");
        assertActiveGroupMember(userId, groupId);

        // Validate split integrity before any writes.
        List<SplitAssignment> splitAssignments = validateSplit(groupId, request.getSplit(), request.getAmount());

        // Mutating operations:

        // Persist parent expense row.
        Expense expense = Expense.builder()
                .issuer(entityManager.getReference(User.class, userId))
                .group(entityManager.getReference(Group.class, groupId))
                .amount(request.getAmount())
                .build();
        Expense savedExpense = expenseRepository.save(expense);

        // Persist child share rows (one per split assignment).
        List<Share> shares = splitAssignments.stream()
                .map(assignment -> Share.builder()
                        .expense(savedExpense)
                        .user(entityManager.getReference(User.class, assignment.userId()))
                        .amount(assignment.amount())
                        .build())
                .toList();
        shareRepository.saveAll(shares);

        expenseEventPublisher.expenseCreated(savedExpense);

        // Serialize and return results:

        return toExpenseResponse(savedExpense, shares);
    }

    /**
     * Returns all expenses for a group if the caller is currently an active member.
     */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getByGroup(UUID groupId, UUID requestingUserId) {
        
        // Preconditions:

        // Authorize group visibility.
        assertActiveGroupMember(requestingUserId, groupId);

        // Read:

        // Query expenses and project with split details.
        List<Expense> expenses = expenseRepository.findByGroup_Id(groupId);

        // Serialize and return results:

        return expenses.stream()
                .map(expense -> toExpenseResponse(expense, shareRepository.findByExpense_Id(expense.getId())))
                .toList();
    }

    /**
     * Returns a single expense if it exists and caller is an active member of its group.
     */
    @Transactional(readOnly = true)
    public ExpenseResponse getById(UUID expenseId, UUID requestingUserId) {

        // Load expense if possible
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));

        // Ensure visibility
        assertActiveGroupMember(requestingUserId, expense.getGroup().getId());

        // Find share data:
        List<Share> shares = shareRepository.findByExpense_Id(expense.getId());

        // Serialize and return results:
        return toExpenseResponse(expense, shares);
    }

    /**
     * Updates expense amount/group/split with ownership and optimistic-version checks.
     * 
     * Key function to get right correctness wise
     */
    @Transactional
    public ExpenseResponse update(UUID expenseId, UUID userId, ExpenseRequest request) {
        // Read in expense:
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));

        // Ensure precondition:
        assertExpenseOwner(userId, expense);

        // Optimistic concurrency check:

        // We cant just leave it to the automatic JPA check, we must also check against 
        // the version in the request body, as the request might not be as fresh as the 
        // latest update to the expense.
        assertVersionMatches(request.getVersion(), expense.getVersion());

        // Disallow updates once settlements exist:
        if (settlementRepository.existsByExpense_Id(expenseId)) {
            throw new ConflictException("Cannot update expense after settlements exist");
        }

        // Ensure active user
        UUID groupId = parseUuid(request.getGroupId(), "groupId");
        assertActiveGroupMember(userId, groupId);

        // Ensure request has valid split 
        List<SplitAssignment> splitAssignments = validateSplit(groupId, request.getSplit(), request.getAmount());

        // Make update to expense enity
        expense.setAmount(request.getAmount());
        expense.setGroup(entityManager.getReference(Group.class, groupId));

        // Here the version is checked, and if mismatch we get a 
        // OptimisticLockException/ObjectOptimisticLockingFailureException
        Expense savedExpense = expenseRepository.save(expense);

        // Make update to share entity
        shareRepository.deleteByExpense_Id(expenseId);
        // Force delete execution before inserts to avoid (expense_id, user_id) unique collisions.
        shareRepository.flush();
        List<Share> shares = splitAssignments.stream()
                .map(assignment -> Share.builder()
                        .expense(savedExpense)
                        .user(entityManager.getReference(User.class, assignment.userId()))
                        .amount(assignment.amount())
                        .build())
                .toList();
        shareRepository.saveAll(shares);

        expenseEventPublisher.expenseUpdated(savedExpense);

        // Serialize and return update
        return toExpenseResponse(savedExpense, shares);
    }

    /**
     * Deletes an expense after ownership and optimistic-version checks.
     */
    @Transactional
    public void delete(UUID expenseId, UUID userId, Long version) {
        // Find expense
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));

        // Only owner can delete an expense
        assertExpenseOwner(userId, expense);

        // We cant just leave it to the automatic JPA check, we must also check against 
        // the version in the request, as the request might not be as fresh as the 
        // latest update to the expense.
        assertVersionMatches(version, expense.getVersion());

        //Delete:
        settlementRepository.deleteByExpense_Id(expenseId);
        shareRepository.deleteByExpense_Id(expenseId);
        expenseRepository.delete(expense);
        expenseEventPublisher.expenseDeleted(expense);
    }

    /**
     * Ensures userId is active member in groupId
     */
    private void assertActiveGroupMember(UUID userId, UUID groupId) {
        boolean activeMember = groupMemberRepository.existsByGroup_IdAndUser_IdAndActiveTrue(groupId, userId);
        if (!activeMember) {
            throw new ForbiddenException("Caller is not an active member of this group");
        }
    }

    /**
     * Ensures userId is the owner of expense
     */
    private void assertExpenseOwner(UUID userId, Expense expense) {
        if (!expense.getIssuer().getId().equals(userId)) {
            throw new ForbiddenException("Caller does not own this expense");
        }
    }

    /**
     * Ensures request version is not-null and matches currentVersion.
     */
    private void assertVersionMatches(Long requestVersion, Long currentVersion) {
        if (requestVersion == null) {
            throw new IllegalArgumentException("version is required");
        }
        if (!requestVersion.equals(currentVersion)) {
            throw new ConflictException("Version mismatch");
        }
    }

    private List<SplitAssignment> validateSplit(UUID groupId,
                                                List<ExpenseRequest.SplitEntry> split,
                                                BigDecimal totalAmount) {
        // Ensure this is a valid split
        if (split == null || split.isEmpty()) {
            throw new IllegalArgumentException("split must be present and non-empty");
        }
        if (totalAmount == null || totalAmount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        Set<UUID> uniqueUsers = new HashSet<>();
        List<SplitAssignment> assignments = new ArrayList<>();
        BigDecimal splitSum = BigDecimal.ZERO;

        for (ExpenseRequest.SplitEntry entry : split) {
            if (entry == null) {
                throw new IllegalArgumentException("split entries cannot be null");
            }
            if (entry.getAmount() == null || entry.getAmount().signum() <= 0) {
                throw new IllegalArgumentException("split amounts must be greater than zero");
            }
            UUID splitUserId = parseUuid(entry.getUserId(), "split.userId");
            if (!uniqueUsers.add(splitUserId)) {
                throw new IllegalArgumentException("split contains duplicate user IDs");
            }
            assignments.add(new SplitAssignment(splitUserId, entry.getAmount()));
            splitSum = splitSum.add(entry.getAmount());
        }

        // Ensure the split adds up to totalAmount
        if (splitSum.compareTo(totalAmount) != 0) {
            throw new IllegalArgumentException("sum(split.amount) must equal amount");
        }

        // Ensure all splits are to active group members.
        long activeMembersCount = groupMemberRepository
                .countByGroup_IdAndUser_IdInAndActiveTrue(groupId, uniqueUsers);
        if (activeMembersCount != uniqueUsers.size()) {
            throw new ForbiddenException("All split users must be active group members");
        }

        return assignments;
    }

    /**
     * Builds API response payload from persisted Expense and Share rows.
     */
    private ExpenseResponse toExpenseResponse(Expense expense, Collection<Share> shares) {
        ExpenseResponse response = new ExpenseResponse();
        response.setId(expense.getId().toString());
        response.setGroupId(expense.getGroup().getId().toString());
        response.setUserId(expense.getIssuer().getId().toString());
        response.setAmount(expense.getAmount());
        response.setVersion(expense.getVersion());
        response.setSplit(shares.stream().map(this::toSplitEntry).toList());
        return response;
    }

    /**
     * Converts a persisted share row to API split payload format.
     */
    private ExpenseRequest.SplitEntry toSplitEntry(Share share) {
        ExpenseRequest.SplitEntry splitEntry = new ExpenseRequest.SplitEntry();
        splitEntry.setUserId(share.getUser().getId().toString());
        splitEntry.setAmount(share.getAmount());
        return splitEntry;
    }

    /**
     * Parses and validates UUID inputs from API layer.
     */
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

    private record SplitAssignment(UUID userId, BigDecimal amount) {}
}
