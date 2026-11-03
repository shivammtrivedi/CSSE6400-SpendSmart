package com.spendmart.group_service.service;

import com.spendmart.group_service.dto.GroupDetailResponse;
import com.spendmart.group_service.dto.GroupResponse;
import com.spendmart.group_service.dto.GroupSummary;
import com.spendmart.group_service.repository.GroupMemberRepository;
import com.spendmart.group_service.repository.GroupRepository;
import com.spendmart.group_service.repository.UserRepository;
import com.spendmart.schema.entity.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class GroupServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired private GroupService groupService;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;

    @BeforeEach
    void clean() {
        groupMemberRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User persistUser(String name) {
        return userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email(name + "@example.com")
                .name(name)
                .build());
    }

    @Test
    void createGroup_persistsGroupAndOwnerAsActiveMember() {
        User alice = persistUser("alice");

        GroupResponse response = groupService.createGroup(alice.getId().toString(), "Trip");

        assertThat(response.groupId()).isNotBlank();
        assertThat(response.name()).isEqualTo("Trip");
        assertThat(response.createdBy()).isEqualTo(alice.getId().toString());
        assertThat(response.createdAt()).isNotNull();

        UUID gid = UUID.fromString(response.groupId());
        assertThat(groupMemberRepository.existsByGroup_IdAndUser_IdAndActiveTrue(gid, alice.getId())).isTrue();
    }

    @Test
    void listGroups_returnsOnlyGroupsCallerIsActiveIn() {
        User alice = persistUser("alice");
        groupService.createGroup(alice.getId().toString(), "Trip");
        groupService.createGroup(alice.getId().toString(), "Flatmates");

        assertThat(groupService.listGroups(alice.getId().toString()).groups())
                .extracting(GroupSummary::name)
                .containsExactlyInAnyOrder("Trip", "Flatmates");
    }

    @Test
    void addMember_thenGetGroup_listsBothActiveMembers() {
        User alice = persistUser("alice");
        User bob = persistUser("bob");
        String gid = groupService.createGroup(alice.getId().toString(), "Trip").groupId();

        groupService.addMember(gid, alice.getId().toString(), bob.getId().toString());

        GroupDetailResponse detail = groupService.getGroup(gid, alice.getId().toString());
        assertThat(detail.name()).isEqualTo("Trip");
        assertThat(detail.members())
                .containsExactlyInAnyOrder(alice.getId().toString(), bob.getId().toString());
    }

    @Test
    void deactivateMember_removesUserFromActiveMembers() {
        User alice = persistUser("alice");
        User bob = persistUser("bob");
        String gid = groupService.createGroup(alice.getId().toString(), "Trip").groupId();
        groupService.addMember(gid, alice.getId().toString(), bob.getId().toString());

        groupService.deactivateMember(gid, alice.getId().toString(), bob.getId().toString());

        assertThat(groupService.getGroup(gid, alice.getId().toString()).members())
                .containsExactly(alice.getId().toString());
        assertThat(groupService.listGroups(bob.getId().toString()).groups()).isEmpty();
    }

    @Test
    void deactivateMember_cannotRemoveOwner_soGroupNeverEmpties() {
        User alice = persistUser("alice");
        String gid = groupService.createGroup(alice.getId().toString(), "Trip").groupId();

        assertThatThrownBy(() -> groupService.deactivateMember(gid, alice.getId().toString(), alice.getId().toString()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(groupService.getGroup(gid, alice.getId().toString()).members())
                .containsExactly(alice.getId().toString());
    }

    @Test
    void getGroup_throws403_whenCallerNotMember() {
        User alice = persistUser("alice");
        User stranger = persistUser("stranger");
        String gid = groupService.createGroup(alice.getId().toString(), "Trip").groupId();

        assertThatThrownBy(() -> groupService.getGroup(gid, stranger.getId().toString()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void addMember_throws404_whenTargetUserDoesNotExist() {
        User alice = persistUser("alice");
        String gid = groupService.createGroup(alice.getId().toString(), "Trip").groupId();

        assertThatThrownBy(() -> groupService.addMember(
                gid, alice.getId().toString(), UUID.randomUUID().toString()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteGroup_disbands_hiddenFromListAndGet() {
        User alice = persistUser("alice");
        String gid = groupService.createGroup(alice.getId().toString(), "Trip").groupId();

        groupService.deleteGroup(gid, alice.getId().toString());

        assertThat(groupService.listGroups(alice.getId().toString()).groups()).isEmpty();
        assertThatThrownBy(() -> groupService.getGroup(gid, alice.getId().toString()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteGroup_throws403_whenNotOwner() {
        User alice = persistUser("alice");
        User bob = persistUser("bob");
        String gid = groupService.createGroup(alice.getId().toString(), "Trip").groupId();
        groupService.addMember(gid, alice.getId().toString(), bob.getId().toString());

        assertThatThrownBy(() -> groupService.deleteGroup(gid, bob.getId().toString()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void addMember_throws403_whenCallerIsMemberButNotOwner() {
        User alice = persistUser("alice");
        User bob = persistUser("bob");
        User carol = persistUser("carol");
        String gid = groupService.createGroup(alice.getId().toString(), "Trip").groupId();
        groupService.addMember(gid, alice.getId().toString(), bob.getId().toString());

        assertThatThrownBy(() -> groupService.addMember(gid, bob.getId().toString(), carol.getId().toString()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
