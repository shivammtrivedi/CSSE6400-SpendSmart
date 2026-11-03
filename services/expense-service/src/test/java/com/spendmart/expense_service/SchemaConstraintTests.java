package com.spendmart.expense_service;

import com.spendmart.schema.entity.Expense;
import com.spendmart.schema.entity.Group;
import com.spendmart.schema.entity.Share;
import com.spendmart.schema.entity.Transaction;
import com.spendmart.schema.entity.User;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Testcontainers
@EntityScan(basePackages = "com.spendmart.schema.entity")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class SchemaConstraintTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void transactionExternalTxnIdMustBeUnique() {
        User owner = User.builder()
                .id(UUID.randomUUID())
                .email("owner1@example.com")
                .name("Owner One")
                .build();
        entityManager.persist(owner);

        Transaction txn1 = Transaction.builder()
                .owner(owner)
                .externalTxnId("obp-txn-123")
                .provider("obp")
                .amount(BigDecimal.valueOf(10))
                .currency("AUD")
                .occurredAt(LocalDateTime.now())
                .build();
        entityManager.persist(txn1);
        entityManager.flush();

        Transaction txn2 = Transaction.builder()
                .owner(owner)
                .externalTxnId("obp-txn-123")
                .provider("obp")
                .amount(BigDecimal.valueOf(20))
                .currency("AUD")
                .occurredAt(LocalDateTime.now())
                .build();
        entityManager.persist(txn2);

        assertThrows(PersistenceException.class, entityManager::flush);
    }

    @Test
    void shareMustBeUniquePerExpenseAndUser() {
        User owner = User.builder()
                .id(UUID.randomUUID())
                .email("owner2@example.com")
                .name("Owner Two")
                .build();
        User participant = User.builder()
                .id(UUID.randomUUID())
                .email("participant@example.com")
                .name("Participant")
                .build();
        entityManager.persist(owner);
        entityManager.persist(participant);

        Group group = Group.builder()
                .name("Shared House")
                .owner(owner)
                .build();
        entityManager.persist(group);

        Expense expense = Expense.builder()
                .issuer(owner)
                .group(group)
                .amount(BigDecimal.valueOf(50))
                .build();
        entityManager.persist(expense);
        entityManager.flush();

        Share share1 = Share.builder()
                .expense(expense)
                .user(participant)
                .amount(BigDecimal.valueOf(25))
                .build();
        entityManager.persist(share1);
        entityManager.flush();

        Share share2 = Share.builder()
                .expense(expense)
                .user(participant)
                .amount(BigDecimal.valueOf(25))
                .build();
        entityManager.persist(share2);

        assertThrows(PersistenceException.class, entityManager::flush);
    }
}
