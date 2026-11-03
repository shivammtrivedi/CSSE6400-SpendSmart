package com.spendmart.expense_service.service;

import com.spendmart.expense_service.dto.ExpenseEventType;
import com.spendmart.expense_service.dto.GroupStreamEvent;
import com.spendmart.expense_service.dto.ExpenseStreamEvent;
import com.spendmart.expense_service.dto.SettlementEventType;
import com.spendmart.expense_service.dto.SettlementStreamEvent;
import com.spendmart.schema.entity.Expense;
import com.spendmart.schema.entity.Settlement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@Service
public class ExpenseEventPublisher {
    private final SseSubscriptionManager subscriptionManager;

    public ExpenseEventPublisher(SseSubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    public void expenseCreated(Expense expense) {
        publishAfterCommit(new ExpenseStreamEvent(
                ExpenseEventType.EXPENSE_CREATED,
                expense.getGroup().getId(),
                expense.getId(),
                expense.getVersion(),
                Instant.now()
        ));
    }

    public void expenseUpdated(Expense expense) {
        publishAfterCommit(new ExpenseStreamEvent(
                ExpenseEventType.EXPENSE_UPDATED,
                expense.getGroup().getId(),
                expense.getId(),
                expense.getVersion(),
                Instant.now()
        ));
    }

    public void expenseDeleted(Expense expense) {
        publishAfterCommit(new ExpenseStreamEvent(
                ExpenseEventType.EXPENSE_DELETED,
                expense.getGroup().getId(),
                expense.getId(),
                expense.getVersion(),
                Instant.now()
        ));
    }

    public void settlementCreated(Settlement settlement) {
        publishAfterCommit(new SettlementStreamEvent(
                SettlementEventType.SETTLEMENT_CREATED,
                settlement.getExpense().getGroup().getId(),
                settlement.getExpense().getId(),
                settlement.getLinkId(),
                settlement.getVersion(),
                Instant.now()
        ));
    }

    public void settlementDeleted(Settlement settlement) {
        publishAfterCommit(new SettlementStreamEvent(
                SettlementEventType.SETTLEMENT_DELETED,
                settlement.getExpense().getGroup().getId(),
                settlement.getExpense().getId(),
                settlement.getLinkId(),
                settlement.getVersion(),
                Instant.now()
        ));
    }

    private void publishAfterCommit(GroupStreamEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    subscriptionManager.publish(event.groupId(), event);
                }
            });
            return;
        }
        subscriptionManager.publish(event.groupId(), event);
    }
}
