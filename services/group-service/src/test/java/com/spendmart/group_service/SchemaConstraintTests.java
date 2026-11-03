package com.spendmart.group_service;

import com.spendmart.schema.entity.Group;
import com.spendmart.schema.entity.GroupMember;
import com.spendmart.schema.entity.User;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
    void groupMemberMustBeUniquePerGroupAndUser() {
        User owner = User.builder()
                .id(UUID.randomUUID())
                .email("owner-group@example.com")
                .name("Owner")
                .build();
        User member = User.builder()
                .id(UUID.randomUUID())
                .email("member-group@example.com")
                .name("Member")
                .build();
        entityManager.persist(owner);
        entityManager.persist(member);

        Group group = Group.builder()
                .name("Trip")
                .owner(owner)
                .build();
        entityManager.persist(group);
        entityManager.flush();

        GroupMember gm1 = GroupMember.builder()
                .group(group)
                .user(member)
                .role("MEMBER")
                .active(true)
                .build();
        entityManager.persist(gm1);
        entityManager.flush();

        GroupMember gm2 = GroupMember.builder()
                .group(group)
                .user(member)
                .role("MEMBER")
                .active(true)
                .build();
        entityManager.persist(gm2);

        assertThrows(PersistenceException.class, entityManager::flush);
    }
}
