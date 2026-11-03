package com.spendmart.expense_service;

import com.spendmart.expense_service.dto.ExpenseRequest;
import com.spendmart.expense_service.exception.ConflictException;
import com.spendmart.expense_service.exception.ForbiddenException;
import com.spendmart.expense_service.repository.ExpenseRepository;
import com.spendmart.expense_service.repository.GroupMemberRepository;
import com.spendmart.expense_service.repository.SettlementRepository;
import com.spendmart.expense_service.repository.ShareRepository;
import com.spendmart.expense_service.service.ExpenseService;
import com.spendmart.schema.entity.Expense;
import com.spendmart.schema.entity.Group;
import com.spendmart.schema.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceValidationTests {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ShareRepository shareRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ExpenseService expenseService;

    @Test
    void createFailsWhenCallerIsNotActiveGroupMember() {
        UUID callerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        ExpenseRequest request = expenseRequest(groupId.toString(), BigDecimal.TEN, BigDecimal.TEN);

        when(groupMemberRepository.existsByGroup_IdAndUser_IdAndActiveTrue(groupId, callerId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> expenseService.create(callerId, request));
    }

    @Test
    void createFailsWhenSplitDoesNotMatchAmount() {
        UUID callerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        ExpenseRequest request = expenseRequest(groupId.toString(), BigDecimal.valueOf(20), BigDecimal.TEN);

        when(groupMemberRepository.existsByGroup_IdAndUser_IdAndActiveTrue(groupId, callerId)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> expenseService.create(callerId, request));
    }

    @Test
    void updateFailsWhenVersionIsStale() {
        UUID callerId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        Expense expense = Expense.builder()
                .id(expenseId)
                .version(5L)
                .issuer(User.builder().id(callerId).email("owner@example.com").name("Owner").build())
                .group(Group.builder().id(groupId).name("Trip").owner(User.builder().id(callerId).email("owner@example.com").name("Owner").build()).build())
                .amount(BigDecimal.TEN)
                .build();

        ExpenseRequest request = expenseRequest(groupId.toString(), BigDecimal.TEN, BigDecimal.TEN);
        request.setVersion(4L);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        assertThrows(ConflictException.class, () -> expenseService.update(expenseId, callerId, request));
    }

    private ExpenseRequest expenseRequest(String groupId, BigDecimal total, BigDecimal splitAmount) {
        ExpenseRequest request = new ExpenseRequest();
        request.setGroupId(groupId);
        request.setAmount(total);

        ExpenseRequest.SplitEntry splitEntry = new ExpenseRequest.SplitEntry();
        splitEntry.setUserId(UUID.randomUUID().toString());
        splitEntry.setAmount(splitAmount);
        request.setSplit(List.of(splitEntry));

        return request;
    }
}
