package com.spendmart.expense_service;

import com.spendmart.expense_service.dto.SettlementRequest;
import com.spendmart.expense_service.dto.SettlementResponse;
import com.spendmart.expense_service.exception.ConflictException;
import com.spendmart.expense_service.exception.ForbiddenException;
import com.spendmart.expense_service.repository.ExpenseRepository;
import com.spendmart.expense_service.repository.SettlementRepository;
import com.spendmart.expense_service.repository.ShareRepository;
import com.spendmart.expense_service.repository.TransactionRepository;
import com.spendmart.expense_service.service.ExpenseEventPublisher;
import com.spendmart.expense_service.service.SettlementService;
import com.spendmart.schema.entity.Expense;
import com.spendmart.schema.entity.Group;
import com.spendmart.schema.entity.Settlement;
import com.spendmart.schema.entity.Share;
import com.spendmart.schema.entity.Transaction;
import com.spendmart.schema.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class SettlementServiceTest {

    @Mock private EntityManager entityManager;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ShareRepository shareRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private ExpenseEventPublisher expenseEventPublisher;

    @InjectMocks private SettlementService settlementService;

    private static final UUID CALLER     = UUID.randomUUID();
    private static final UUID EXPENSE_ID = UUID.randomUUID();
    private static final UUID LINK_ID    = UUID.randomUUID();
    private static final String PROVIDER = "obp";
    private static final String TXN_ID   = "obp-txn-abc";

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User user(UUID id) {
        return User.builder().id(id).email(id + "@test.com").name("U").build();
    }

    private Expense expense(long version) {
        return Expense.builder()
                .id(EXPENSE_ID)
                .issuer(user(CALLER))
                .group(Group.builder().id(UUID.randomUUID()).name("G").owner(user(CALLER)).build())
                .amount(BigDecimal.valueOf(100))
                .version(version)
                .build();
    }

    private Share share() {
        return Share.builder()
                .expense(expense(0L))
                .user(user(CALLER))
                .amount(BigDecimal.valueOf(50))
                .build();
    }

    private Transaction transaction() {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .owner(user(CALLER))
                .externalTxnId(TXN_ID)
                .provider(PROVIDER)
                .amount(BigDecimal.valueOf(50))
                .currency("AUD")
                .occurredAt(LocalDateTime.now())
                .build();
    }

    private Settlement settlement(UUID linkId, long version) {
        return Settlement.builder()
                .id(UUID.randomUUID())
                .linkId(linkId)
                .expense(expense(0L))
                .user(user(CALLER))
                .transaction(transaction())
                .share(share())
                .amount(BigDecimal.valueOf(50))
                .version(version)
                .build();
    }

    private SettlementRequest request(long version) {
        SettlementRequest req = new SettlementRequest();
        req.setVersion(version);
        req.setProvider(PROVIDER);
        req.setTransactionId(TXN_ID);
        req.setAmount(BigDecimal.valueOf(50));
        return req;
    }

    // -------------------------------------------------------------------------
    // getByExpense
    // -------------------------------------------------------------------------

    @Test
    void getByExpense_returnsSettlements_whenCallerIsOnExpense() {
        Expense exp = expense(0L);
        Share s = share();
        Settlement stl = settlement(LINK_ID, 0L);

        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(Optional.of(exp));
        when(shareRepository.findByExpense_IdAndUser_Id(EXPENSE_ID, CALLER)).thenReturn(Optional.of(s));
        when(settlementRepository.findByExpense_Id(EXPENSE_ID)).thenReturn(List.of(stl));

        List<SettlementResponse> result = settlementService.getByExpense(EXPENSE_ID, CALLER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLinkId()).isEqualTo(LINK_ID.toString());
    }

    @Test
    void getByExpense_throws404_whenExpenseNotFound() {
        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.getByExpense(EXPENSE_ID, CALLER))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getByExpense_throws403_whenCallerNotOnExpense() {
        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(Optional.of(expense(0L)));
        when(shareRepository.findByExpense_IdAndUser_Id(EXPENSE_ID, CALLER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.getByExpense(EXPENSE_ID, CALLER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getByExpense_returnsEmptyList_whenNoSettlementsExist() {
        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(Optional.of(expense(0L)));
        when(shareRepository.findByExpense_IdAndUser_Id(EXPENSE_ID, CALLER)).thenReturn(Optional.of(share()));
        when(settlementRepository.findByExpense_Id(EXPENSE_ID)).thenReturn(List.of());

        List<SettlementResponse> result = settlementService.getByExpense(EXPENSE_ID, CALLER);

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void create_succeeds_persistsSettlementAndPublishesEvent() {
        Share userShare = share();
        Transaction txn = transaction();
        Settlement saved = settlement(UUID.randomUUID(), 0L);

        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(Optional.of(expense(0L)));
        when(shareRepository.findByExpense_IdAndUser_Id(EXPENSE_ID, CALLER)).thenReturn(Optional.of(userShare));
        when(transactionRepository.findByExternalTxnIdAndOwner_IdAndProvider(TXN_ID, CALLER, PROVIDER))
                .thenReturn(Optional.of(txn));
        when(entityManager.getReference(User.class, CALLER)).thenReturn(user(CALLER));
        when(settlementRepository.save(any())).thenReturn(saved);

        SettlementResponse resp = settlementService.create(EXPENSE_ID, CALLER, request(0L));

        assertThat(resp).isNotNull();
        verify(settlementRepository).save(any(Settlement.class));
        verify(expenseEventPublisher).settlementCreated(saved);
    }

    @Test
    void create_throws404_whenExpenseNotFound() {
        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.create(EXPENSE_ID, CALLER, request(0L)))
                .isInstanceOf(EntityNotFoundException.class);
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void create_throws403_whenCallerNotOnExpense() {
        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(Optional.of(expense(0L)));
        when(shareRepository.findByExpense_IdAndUser_Id(EXPENSE_ID, CALLER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.create(EXPENSE_ID, CALLER, request(0L)))
                .isInstanceOf(ForbiddenException.class);
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void create_throws409_whenVersionIsStale() {
        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(Optional.of(expense(5L)));
        when(shareRepository.findByExpense_IdAndUser_Id(EXPENSE_ID, CALLER)).thenReturn(Optional.of(share()));

        assertThatThrownBy(() -> settlementService.create(EXPENSE_ID, CALLER, request(4L)))
                .isInstanceOf(ConflictException.class);
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void create_throws404_whenTransactionNotFound() {
        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(Optional.of(expense(0L)));
        when(shareRepository.findByExpense_IdAndUser_Id(EXPENSE_ID, CALLER)).thenReturn(Optional.of(share()));
        when(transactionRepository.findByExternalTxnIdAndOwner_IdAndProvider(TXN_ID, CALLER, PROVIDER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.create(EXPENSE_ID, CALLER, request(0L)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(PROVIDER);
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void create_throws_whenVersionIsNull() {
        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(Optional.of(expense(0L)));
        when(shareRepository.findByExpense_IdAndUser_Id(EXPENSE_ID, CALLER)).thenReturn(Optional.of(share()));

        SettlementRequest req = request(0L);
        req.setVersion(null);

        assertThatThrownBy(() -> settlementService.create(EXPENSE_ID, CALLER, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void delete_succeeds_deletesAndPublishesEvent() {
        Settlement stl = settlement(LINK_ID, 0L);
        when(settlementRepository.findByLinkIdAndExpense_Id(LINK_ID, EXPENSE_ID)).thenReturn(Optional.of(stl));

        settlementService.delete(EXPENSE_ID, LINK_ID, CALLER, 0L);

        verify(settlementRepository).delete(stl);
        verify(expenseEventPublisher).settlementDeleted(stl);
    }

    @Test
    void delete_throws404_whenSettlementNotFound() {
        when(settlementRepository.findByLinkIdAndExpense_Id(LINK_ID, EXPENSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.delete(EXPENSE_ID, LINK_ID, CALLER, 0L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(settlementRepository, never()).delete(any());
    }

    @Test
    void delete_throws403_whenCallerNotSettlementOwner() {
        UUID stranger = UUID.randomUUID();
        Settlement stl = settlement(LINK_ID, 0L);
        when(settlementRepository.findByLinkIdAndExpense_Id(LINK_ID, EXPENSE_ID)).thenReturn(Optional.of(stl));

        assertThatThrownBy(() -> settlementService.delete(EXPENSE_ID, LINK_ID, stranger, 0L))
                .isInstanceOf(ForbiddenException.class);
        verify(settlementRepository, never()).delete(any());
    }

    @Test
    void delete_throws409_whenVersionIsStale() {
        Settlement stl = settlement(LINK_ID, 3L);
        when(settlementRepository.findByLinkIdAndExpense_Id(LINK_ID, EXPENSE_ID)).thenReturn(Optional.of(stl));

        assertThatThrownBy(() -> settlementService.delete(EXPENSE_ID, LINK_ID, CALLER, 2L))
                .isInstanceOf(ConflictException.class);
        verify(settlementRepository, never()).delete(any());
    }

    @Test
    void delete_throws_whenVersionIsNull() {
        Settlement stl = settlement(LINK_ID, 0L);
        when(settlementRepository.findByLinkIdAndExpense_Id(LINK_ID, EXPENSE_ID)).thenReturn(Optional.of(stl));

        assertThatThrownBy(() -> settlementService.delete(EXPENSE_ID, LINK_ID, CALLER, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
