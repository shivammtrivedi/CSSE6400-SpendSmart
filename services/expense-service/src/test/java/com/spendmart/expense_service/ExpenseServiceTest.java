package com.spendmart.expense_service;

import com.spendmart.expense_service.dto.ExpenseRequest;
import com.spendmart.expense_service.dto.ExpenseResponse;
import com.spendmart.expense_service.exception.ConflictException;
import com.spendmart.expense_service.exception.ForbiddenException;
import com.spendmart.expense_service.repository.ExpenseRepository;
import com.spendmart.expense_service.repository.GroupMemberRepository;
import com.spendmart.expense_service.repository.SettlementRepository;
import com.spendmart.expense_service.repository.ShareRepository;
import com.spendmart.expense_service.service.ExpenseEventPublisher;
import com.spendmart.expense_service.service.ExpenseService;
import com.spendmart.schema.entity.Expense;
import com.spendmart.schema.entity.Group;
import com.spendmart.schema.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private ShareRepository shareRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private EntityManager entityManager;
    @Mock private ExpenseEventPublisher expenseEventPublisher;

    @InjectMocks private ExpenseService expenseService;

    private static final UUID CALLER  = UUID.randomUUID();
    private static final UUID GROUP   = UUID.randomUUID();
    private static final UUID SPLIT_USER = UUID.randomUUID();
    private static final UUID EXPENSE = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User user(UUID id) {
        return User.builder().id(id).email(id + "@test.com").name("User").build();
    }

    private Group group(UUID id, UUID ownerId) {
        return Group.builder().id(id).name("G").owner(user(ownerId)).build();
    }

    private Expense expense(UUID id, UUID ownerId, UUID groupId, long version) {
        return Expense.builder()
                .id(id)
                .issuer(user(ownerId))
                .group(group(groupId, ownerId))
                .amount(BigDecimal.TEN)
                .version(version)
                .build();
    }

    private ExpenseRequest request(BigDecimal total, BigDecimal splitAmt, long version) {
        ExpenseRequest req = new ExpenseRequest();
        req.setGroupId(GROUP.toString());
        req.setAmount(total);
        req.setVersion(version);
        ExpenseRequest.SplitEntry entry = new ExpenseRequest.SplitEntry();
        entry.setUserId(SPLIT_USER.toString());
        entry.setAmount(splitAmt);
        req.setSplit(List.of(entry));
        return req;
    }

    private void stubActiveMember(UUID groupId, UUID userId) {
        when(groupMemberRepository.existsByGroup_IdAndUser_IdAndActiveTrue(groupId, userId)).thenReturn(true);
    }

    private void stubSplitMembersValid() {
        when(groupMemberRepository.countByGroup_IdAndUser_IdInAndActiveTrue(any(), any())).thenReturn(1L);
    }

    private Expense savedExpense() {
        Expense e = expense(EXPENSE, CALLER, GROUP, 0L);
        when(expenseRepository.save(any())).thenReturn(e);
        when(entityManager.getReference(User.class, CALLER)).thenReturn(user(CALLER));
        when(entityManager.getReference(User.class, SPLIT_USER)).thenReturn(user(SPLIT_USER));
        when(entityManager.getReference(Group.class, GROUP)).thenReturn(group(GROUP, CALLER));
        return e;
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void create_succeeds_persistsSharesAndPublishesEvent() {
        stubActiveMember(GROUP, CALLER);
        stubSplitMembersValid();
        Expense saved = savedExpense();

        ExpenseResponse resp = expenseService.create(CALLER, request(BigDecimal.TEN, BigDecimal.TEN, 0L));

        assertThat(resp.getAmount()).isEqualByComparingTo(BigDecimal.TEN);
        verify(shareRepository).saveAll(any());
        verify(expenseEventPublisher).expenseCreated(saved);
    }

    @Test
    void create_throws403_whenCallerNotActiveMember() {
        when(groupMemberRepository.existsByGroup_IdAndUser_IdAndActiveTrue(GROUP, CALLER)).thenReturn(false);

        assertThatThrownBy(() -> expenseService.create(CALLER, request(BigDecimal.TEN, BigDecimal.TEN, 0L)))
                .isInstanceOf(ForbiddenException.class);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void create_throws_whenSplitDoesNotSumToTotal() {
        stubActiveMember(GROUP, CALLER);

        assertThatThrownBy(() -> expenseService.create(CALLER, request(BigDecimal.TEN, BigDecimal.valueOf(7), 0L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_throws_whenSplitSumExceedsTotal() {
        stubActiveMember(GROUP, CALLER);

        assertThatThrownBy(() -> expenseService.create(CALLER, request(BigDecimal.TEN, BigDecimal.valueOf(15), 0L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_throws403_whenSplitUserNotActiveGroupMember() {
        stubActiveMember(GROUP, CALLER);
        when(groupMemberRepository.countByGroup_IdAndUser_IdInAndActiveTrue(any(), any())).thenReturn(0L);

        assertThatThrownBy(() -> expenseService.create(CALLER, request(BigDecimal.TEN, BigDecimal.TEN, 0L)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void create_throws_whenSplitIsEmpty() {
        stubActiveMember(GROUP, CALLER);
        ExpenseRequest req = new ExpenseRequest();
        req.setGroupId(GROUP.toString());
        req.setAmount(BigDecimal.TEN);
        req.setSplit(List.of());

        assertThatThrownBy(() -> expenseService.create(CALLER, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void update_succeeds_replacesSharesAndPublishesEvent() {
        Expense existing = expense(EXPENSE, CALLER, GROUP, 0L);
        when(expenseRepository.findById(EXPENSE)).thenReturn(Optional.of(existing));
        when(settlementRepository.existsByExpense_Id(EXPENSE)).thenReturn(false);
        stubActiveMember(GROUP, CALLER);
        stubSplitMembersValid();
        when(entityManager.getReference(Group.class, GROUP)).thenReturn(group(GROUP, CALLER));
        when(entityManager.getReference(User.class, SPLIT_USER)).thenReturn(user(SPLIT_USER));
        when(expenseRepository.save(existing)).thenReturn(existing);

        expenseService.update(EXPENSE, CALLER, request(BigDecimal.valueOf(20), BigDecimal.valueOf(20), 0L));

        verify(shareRepository).deleteByExpense_Id(EXPENSE);
        verify(shareRepository).flush();
        verify(shareRepository).saveAll(any());
        verify(expenseEventPublisher).expenseUpdated(existing);
    }

    @Test
    void update_throws409_whenVersionIsStale() {
        Expense existing = expense(EXPENSE, CALLER, GROUP, 5L);
        when(expenseRepository.findById(EXPENSE)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> expenseService.update(EXPENSE, CALLER, request(BigDecimal.TEN, BigDecimal.TEN, 4L)))
                .isInstanceOf(ConflictException.class);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void update_throws409_whenSettlementsExist() {
        Expense existing = expense(EXPENSE, CALLER, GROUP, 0L);
        when(expenseRepository.findById(EXPENSE)).thenReturn(Optional.of(existing));
        when(settlementRepository.existsByExpense_Id(EXPENSE)).thenReturn(true);

        assertThatThrownBy(() -> expenseService.update(EXPENSE, CALLER, request(BigDecimal.TEN, BigDecimal.TEN, 0L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_throws403_whenCallerNotOwner() {
        UUID stranger = UUID.randomUUID();
        Expense existing = expense(EXPENSE, CALLER, GROUP, 0L);
        when(expenseRepository.findById(EXPENSE)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> expenseService.update(EXPENSE, stranger, request(BigDecimal.TEN, BigDecimal.TEN, 0L)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_throws404_whenExpenseNotFound() {
        when(expenseRepository.findById(EXPENSE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.update(EXPENSE, CALLER, request(BigDecimal.TEN, BigDecimal.TEN, 0L)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void delete_succeeds_cleansUpAndPublishesEvent() {
        Expense existing = expense(EXPENSE, CALLER, GROUP, 0L);
        when(expenseRepository.findById(EXPENSE)).thenReturn(Optional.of(existing));

        expenseService.delete(EXPENSE, CALLER, 0L);

        verify(settlementRepository).deleteByExpense_Id(EXPENSE);
        verify(shareRepository).deleteByExpense_Id(EXPENSE);
        verify(expenseRepository).delete(existing);
        verify(expenseEventPublisher).expenseDeleted(existing);
    }

    @Test
    void delete_throws409_whenVersionIsStale() {
        Expense existing = expense(EXPENSE, CALLER, GROUP, 2L);
        when(expenseRepository.findById(EXPENSE)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> expenseService.delete(EXPENSE, CALLER, 1L))
                .isInstanceOf(ConflictException.class);
        verify(expenseRepository, never()).delete(any());
    }

    @Test
    void delete_throws403_whenCallerNotOwner() {
        UUID stranger = UUID.randomUUID();
        Expense existing = expense(EXPENSE, CALLER, GROUP, 0L);
        when(expenseRepository.findById(EXPENSE)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> expenseService.delete(EXPENSE, stranger, 0L))
                .isInstanceOf(ForbiddenException.class);
        verify(expenseRepository, never()).delete(any());
    }

    @Test
    void delete_throws404_whenExpenseNotFound() {
        when(expenseRepository.findById(EXPENSE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.delete(EXPENSE, CALLER, 0L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // getById
    // -------------------------------------------------------------------------

    @Test
    void getById_returnsExpense_whenCallerIsGroupMember() {
        Expense existing = expense(EXPENSE, CALLER, GROUP, 0L);
        when(expenseRepository.findById(EXPENSE)).thenReturn(Optional.of(existing));
        when(groupMemberRepository.existsByGroup_IdAndUser_IdAndActiveTrue(GROUP, CALLER)).thenReturn(true);
        when(shareRepository.findByExpense_Id(EXPENSE)).thenReturn(List.of());

        ExpenseResponse resp = expenseService.getById(EXPENSE, CALLER);

        assertThat(resp.getId()).isEqualTo(EXPENSE.toString());
    }

    @Test
    void getById_throws403_whenCallerNotGroupMember() {
        Expense existing = expense(EXPENSE, CALLER, GROUP, 0L);
        when(expenseRepository.findById(EXPENSE)).thenReturn(Optional.of(existing));
        when(groupMemberRepository.existsByGroup_IdAndUser_IdAndActiveTrue(GROUP, CALLER)).thenReturn(false);

        assertThatThrownBy(() -> expenseService.getById(EXPENSE, CALLER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getById_throws404_whenExpenseNotFound() {
        when(expenseRepository.findById(EXPENSE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getById(EXPENSE, CALLER))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // getByGroup
    // -------------------------------------------------------------------------

    @Test
    void getByGroup_returnsExpenses_whenCallerIsMember() {
        when(groupMemberRepository.existsByGroup_IdAndUser_IdAndActiveTrue(GROUP, CALLER)).thenReturn(true);
        Expense e1 = expense(UUID.randomUUID(), CALLER, GROUP, 0L);
        Expense e2 = expense(UUID.randomUUID(), CALLER, GROUP, 0L);
        when(expenseRepository.findByGroup_Id(GROUP)).thenReturn(List.of(e1, e2));
        when(shareRepository.findByExpense_Id(any())).thenReturn(List.of());

        List<ExpenseResponse> results = expenseService.getByGroup(GROUP, CALLER);

        assertThat(results).hasSize(2);
    }

    @Test
    void getByGroup_throws403_whenCallerNotMember() {
        when(groupMemberRepository.existsByGroup_IdAndUser_IdAndActiveTrue(GROUP, CALLER)).thenReturn(false);

        assertThatThrownBy(() -> expenseService.getByGroup(GROUP, CALLER))
                .isInstanceOf(ForbiddenException.class);
        verify(expenseRepository, never()).findByGroup_Id(any());
    }
}
