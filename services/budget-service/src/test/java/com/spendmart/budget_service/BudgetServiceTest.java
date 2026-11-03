package com.spendmart.budget_service;

import com.spendmart.budget_service.dto.BudgetRequest;
import com.spendmart.budget_service.dto.BudgetResponse;
import com.spendmart.budget_service.entity.Budget;
import com.spendmart.budget_service.exception.ForbiddenException;
import com.spendmart.budget_service.repository.BudgetRepository;
import com.spendmart.budget_service.service.BudgetService;
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
class BudgetServiceTest {

    @Mock private BudgetRepository budgetRepository;
    @InjectMocks private BudgetService budgetService;

    private static final UUID USER_ID   = UUID.randomUUID();
    private static final UUID BUDGET_ID = UUID.randomUUID();

    private Budget budget(UUID ownerId) {
        return Budget.builder()
                .id(BUDGET_ID)
                .userId(ownerId)
                .category("Food")
                .limitAmount(BigDecimal.valueOf(500))
                .period("monthly")
                .build();
    }

    private BudgetRequest request() {
        BudgetRequest req = new BudgetRequest();
        req.setCategory("Transport");
        req.setLimitAmount(BigDecimal.valueOf(200));
        req.setPeriod("monthly");
        return req;
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void create_persistsAndReturnsResponse() {
        Budget saved = budget(USER_ID);
        when(budgetRepository.save(any(Budget.class))).thenReturn(saved);

        BudgetResponse resp = budgetService.create(USER_ID, request());

        assertThat(resp.id()).isEqualTo(BUDGET_ID);
        assertThat(resp.category()).isEqualTo("Food");
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void create_setsCorrectUserId() {
        Budget saved = budget(USER_ID);
        when(budgetRepository.save(any(Budget.class))).thenReturn(saved);

        budgetService.create(USER_ID, request());

        verify(budgetRepository).save(any(Budget.class));
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
    }

    // -------------------------------------------------------------------------
    // getByUser
    // -------------------------------------------------------------------------

    @Test
    void getByUser_returnsAllBudgetsForUser() {
        Budget b1 = budget(USER_ID);
        Budget b2 = Budget.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .category("Entertainment")
                .limitAmount(BigDecimal.valueOf(100))
                .period("weekly")
                .build();
        when(budgetRepository.findByUserId(USER_ID)).thenReturn(List.of(b1, b2));

        List<BudgetResponse> results = budgetService.getByUser(USER_ID);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(BudgetResponse::category)
                .containsExactlyInAnyOrder("Food", "Entertainment");
    }

    @Test
    void getByUser_returnsEmptyList_whenNoBudgets() {
        when(budgetRepository.findByUserId(USER_ID)).thenReturn(List.of());

        List<BudgetResponse> results = budgetService.getByUser(USER_ID);

        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void update_succeeds_whenCallerIsOwner() {
        Budget existing = budget(USER_ID);
        when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(existing)).thenReturn(existing);

        BudgetResponse resp = budgetService.update(USER_ID, BUDGET_ID, request());

        assertThat(resp.category()).isEqualTo(existing.getCategory());
        verify(budgetRepository).save(existing);
    }

    @Test
    void update_appliesNewFieldValues() {
        Budget existing = budget(USER_ID);
        when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(existing)).thenReturn(existing);

        BudgetRequest req = request();
        req.setCategory("Groceries");
        req.setLimitAmount(BigDecimal.valueOf(350));
        req.setPeriod("weekly");

        budgetService.update(USER_ID, BUDGET_ID, req);

        assertThat(existing.getCategory()).isEqualTo("Groceries");
        assertThat(existing.getLimitAmount()).isEqualByComparingTo(BigDecimal.valueOf(350));
        assertThat(existing.getPeriod()).isEqualTo("weekly");
    }

    @Test
    void update_throws404_whenBudgetNotFound() {
        when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.update(USER_ID, BUDGET_ID, request()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(BUDGET_ID.toString());
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void update_throws403_whenCallerNotOwner() {
        UUID stranger = UUID.randomUUID();
        Budget existing = budget(USER_ID);
        when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> budgetService.update(stranger, BUDGET_ID, request()))
                .isInstanceOf(ForbiddenException.class);
        verify(budgetRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void delete_succeeds_whenCallerIsOwner() {
        Budget existing = budget(USER_ID);
        when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(existing));

        budgetService.delete(USER_ID, BUDGET_ID);

        verify(budgetRepository).delete(existing);
    }

    @Test
    void delete_throws404_whenBudgetNotFound() {
        when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.delete(USER_ID, BUDGET_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(BUDGET_ID.toString());
        verify(budgetRepository, never()).delete(any());
    }

    @Test
    void delete_throws403_whenCallerNotOwner() {
        UUID stranger = UUID.randomUUID();
        Budget existing = budget(USER_ID);
        when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> budgetService.delete(stranger, BUDGET_ID))
                .isInstanceOf(ForbiddenException.class);
        verify(budgetRepository, never()).delete(any());
    }
}
