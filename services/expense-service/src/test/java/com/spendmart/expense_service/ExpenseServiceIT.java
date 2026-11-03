package com.spendmart.expense_service;

import com.spendmart.expense_service.dto.ExpenseRequest;
import com.spendmart.expense_service.dto.ExpenseResponse;
import com.spendmart.expense_service.exception.ConflictException;
import com.spendmart.expense_service.repository.ExpenseRepository;
import com.spendmart.expense_service.repository.GroupMemberRepository;
import com.spendmart.expense_service.repository.ShareRepository;
import com.spendmart.expense_service.service.ExpenseEventPublisher;
import com.spendmart.expense_service.service.ExpenseService;
import com.spendmart.schema.entity.Expense;
import com.spendmart.schema.entity.Group;
import com.spendmart.schema.entity.GroupMember;
import com.spendmart.schema.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Integration tests proving JPA @Version optimistic locking works end-to-end
 * against a real PostgreSQL schema — the primary reliability evidence for the
 * concurrent-write safety claim.
 */
@DataJpaTest
@Testcontainers
@EntityScan(basePackages = "com.spendmart.schema.entity")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Import(ExpenseService.class)
class ExpenseServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired private TestEntityManager em;
    @Autowired private ExpenseService expenseService;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private ShareRepository shareRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;

    @MockBean private ExpenseEventPublisher expenseEventPublisher;

    private UUID userId;
    private UUID groupId;
    private UUID splitUserId;

    @BeforeEach
    void seed() {
        User owner = User.builder()
                .id(UUID.randomUUID())
                .email("owner@it.test")
                .name("Owner")
                .build();
        em.persist(owner);
        userId = owner.getId();

        User splitter = User.builder()
                .id(UUID.randomUUID())
                .email("split@it.test")
                .name("Splitter")
                .build();
        em.persist(splitter);
        splitUserId = splitter.getId();

        Group group = Group.builder()
                .name("IT Group")
                .owner(owner)
                .build();
        em.persist(group);
        groupId = group.getId();

        GroupMember ownerMember = GroupMember.builder()
                .group(group)
                .user(owner)
                .role("OWNER")
                .active(true)
                .build();
        em.persist(ownerMember);

        GroupMember splitMember = GroupMember.builder()
                .group(group)
                .user(splitter)
                .role("MEMBER")
                .active(true)
                .build();
        em.persist(splitMember);

        em.flush();
        em.clear();
    }

    private ExpenseRequest buildRequest(BigDecimal total, long version) {
        ExpenseRequest req = new ExpenseRequest();
        req.setGroupId(groupId.toString());
        req.setAmount(total);
        req.setVersion(version);
        ExpenseRequest.SplitEntry entry = new ExpenseRequest.SplitEntry();
        entry.setUserId(splitUserId.toString());
        entry.setAmount(total);
        req.setSplit(List.of(entry));
        return req;
    }

    @Test
    void create_persistsExpenseAndSharesInDatabase() {
        ExpenseResponse resp = expenseService.create(userId, buildRequest(BigDecimal.TEN, 0L));

        UUID expenseId = UUID.fromString(resp.getId());
        assertThat(expenseRepository.findById(expenseId)).isPresent();
        assertThat(shareRepository.findByExpense_Id(expenseId)).hasSize(1);
        verify(expenseEventPublisher).expenseCreated(any(Expense.class));
    }

    @Test
    void create_initialVersionIsZero() {
        ExpenseResponse resp = expenseService.create(userId, buildRequest(BigDecimal.valueOf(50), 0L));

        assertThat(resp.getVersion()).isEqualTo(0L);
    }

    @Test
    void update_incrementsVersionInDatabase() {
        // Create at version 0
        ExpenseResponse created = expenseService.create(userId, buildRequest(BigDecimal.TEN, 0L));
        assertThat(created.getVersion()).isEqualTo(0L);

        // Update with version 0 → version should become 1
        ExpenseResponse updated = expenseService.update(
                UUID.fromString(created.getId()),
                userId,
                buildRequest(BigDecimal.valueOf(20), 0L));

        assertThat(updated.getVersion()).isEqualTo(1L);

        // Confirm the DB row has version = 1
        Expense persisted = expenseRepository.findById(UUID.fromString(created.getId())).orElseThrow();
        assertThat(persisted.getVersion()).isEqualTo(1L);
    }

    /**
     * This is the key reliability test — proves that the manual assertVersionMatches()
     * guard in ExpenseService (backed by JPA @Version on the Expense entity) correctly
     * rejects a write that carries a stale client-side version, preventing lost updates.
     */
    @Test
    void update_withStaleVersion_throwsConflict() {
        ExpenseResponse created = expenseService.create(userId, buildRequest(BigDecimal.TEN, 0L));
        UUID expenseId = UUID.fromString(created.getId());

        // First update — advances version from 0 to 1
        expenseService.update(expenseId, userId, buildRequest(BigDecimal.valueOf(20), 0L));

        // Second update with the original version (0) — should be rejected as stale
        assertThatThrownBy(() ->
                expenseService.update(expenseId, userId, buildRequest(BigDecimal.valueOf(30), 0L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Version mismatch");
    }

    @Test
    void delete_removesExpenseAndSharesFromDatabase() {
        ExpenseResponse created = expenseService.create(userId, buildRequest(BigDecimal.TEN, 0L));
        UUID expenseId = UUID.fromString(created.getId());

        expenseService.delete(expenseId, userId, 0L);

        assertThat(expenseRepository.findById(expenseId)).isEmpty();
        assertThat(shareRepository.findByExpense_Id(expenseId)).isEmpty();
    }

    @Test
    void delete_withStaleVersion_throwsConflict() {
        ExpenseResponse created = expenseService.create(userId, buildRequest(BigDecimal.TEN, 0L));
        UUID expenseId = UUID.fromString(created.getId());

        // Advance the version first
        expenseService.update(expenseId, userId, buildRequest(BigDecimal.valueOf(20), 0L));

        // Delete with old version 0 — should conflict
        assertThatThrownBy(() -> expenseService.delete(expenseId, userId, 0L))
                .isInstanceOf(ConflictException.class);

        assertThat(expenseRepository.findById(expenseId)).isPresent();
    }

    @Test
    void getByGroup_returnsOnlyExpensesForRequestedGroup() {
        // Two groups, one expense each
        expenseService.create(userId, buildRequest(BigDecimal.TEN, 0L));

        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@it.test")
                .name("Other")
                .build();
        em.persist(otherUser);

        Group otherGroup = Group.builder()
                .name("Other Group")
                .owner(otherUser)
                .build();
        em.persist(otherGroup);

        GroupMember mem1 = GroupMember.builder()
                .group(otherGroup)
                .user(otherUser)
                .role("OWNER")
                .active(true)
                .build();
        GroupMember mem2 = GroupMember.builder()
                .group(otherGroup)
                .user(em.find(User.class, userId))
                .role("MEMBER")
                .active(true)
                .build();
        em.persist(mem1);
        em.persist(mem2);
        em.flush();
        em.clear();

        // Expense for otherGroup — use userId who is now a member of both groups
        ExpenseRequest otherReq = new ExpenseRequest();
        otherReq.setGroupId(otherGroup.getId().toString());
        otherReq.setAmount(BigDecimal.valueOf(25));
        otherReq.setVersion(0L);
        ExpenseRequest.SplitEntry entry = new ExpenseRequest.SplitEntry();
        entry.setUserId(otherUser.getId().toString());
        entry.setAmount(BigDecimal.valueOf(25));
        otherReq.setSplit(List.of(entry));
        expenseService.create(userId, otherReq);

        List<ExpenseResponse> forGroup = expenseService.getByGroup(groupId, userId);
        assertThat(forGroup).hasSize(1);
        assertThat(forGroup.get(0).getGroupId()).isEqualTo(groupId.toString());
    }

    @Test
    void create_sseEventIsPublishedAfterPersist() {
        expenseService.create(userId, buildRequest(BigDecimal.valueOf(100), 0L));

        verify(expenseEventPublisher, atLeastOnce()).expenseCreated(any(Expense.class));
    }
}
