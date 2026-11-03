package com.spendmart.group_service.service;

import com.spendmart.group_service.dto.GroupDetailResponse;
import com.spendmart.group_service.dto.GroupListResponse;
import com.spendmart.group_service.dto.GroupResponse;
import com.spendmart.group_service.dto.GroupSummary;
import com.spendmart.group_service.repository.GroupMemberRepository;
import com.spendmart.group_service.repository.GroupRepository;
import com.spendmart.group_service.repository.UserRepository;
import com.spendmart.schema.entity.Group;
import com.spendmart.schema.entity.GroupMember;
import com.spendmart.schema.entity.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
class GroupServiceTest {

    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private GroupService groupService;

    private static final UUID CALLER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID OTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private User user(UUID id, String name) {
        return User.builder().id(id).email(name + "@example.com").name(name).build();
    }

    private Group group(UUID id, String name, User owner) {
        return Group.builder().id(id).name(name).owner(owner).createdAt(LocalDateTime.of(2026, 6, 1, 12, 0)).build();
    }

    private GroupMember member(Group group, User user, boolean active) {
        return GroupMember.builder().id(UUID.randomUUID()).group(group).user(user).role("MEMBER").active(active).build();
    }

    @Test
    void createGroup_savesGroupAndOwnerMembership_returnsResponse() {
        User owner = user(CALLER_ID, "alice");
        Group saved = group(GROUP_ID, "Trip", owner);
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.of(owner));
        when(groupRepository.save(any(Group.class))).thenReturn(saved);

        GroupResponse response = groupService.createGroup(CALLER_ID.toString(), "Trip");

        assertThat(response.groupId()).isEqualTo(GROUP_ID.toString());
        assertThat(response.name()).isEqualTo("Trip");
        assertThat(response.createdBy()).isEqualTo(CALLER_ID.toString());
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 6, 1, 12, 0).toInstant(ZoneOffset.UTC));

        ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("OWNER");
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(captor.getValue().getUser()).isEqualTo(owner);
    }

    @Test
    void createGroup_throwsNotFound_whenCallerHasNoUserRow() {
        when(userRepository.findById(CALLER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.createGroup(CALLER_ID.toString(), "Trip"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void listGroups_returnsActiveGroupSummaries() {
        User caller = user(CALLER_ID, "alice");
        Group g1 = group(GROUP_ID, "Trip", caller);
        Group g2 = group(UUID.randomUUID(), "Flatmates", caller);
        when(groupMemberRepository.findByUser_IdAndActiveTrue(CALLER_ID))
                .thenReturn(List.of(member(g1, caller, true), member(g2, caller, true)));

        GroupListResponse response = groupService.listGroups(CALLER_ID.toString());

        assertThat(response.groups()).extracting(GroupSummary::name).containsExactly("Trip", "Flatmates");
        assertThat(response.groups().get(0).groupId()).isEqualTo(GROUP_ID.toString());
    }

    @Test
    void getGroup_returnsNameAndActiveMembers_whenCallerIsMember() {
        User caller = user(CALLER_ID, "alice");
        User bob = user(OTHER_ID, "bob");
        Group g = group(GROUP_ID, "Trip", caller);
        when(groupMemberRepository.existsByGroup_IdAndUser_IdAndActiveTrue(GROUP_ID, CALLER_ID)).thenReturn(true);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(g));
        when(groupMemberRepository.findByGroup_IdAndActiveTrue(GROUP_ID))
                .thenReturn(List.of(member(g, caller, true), member(g, bob, true)));

        GroupDetailResponse response = groupService.getGroup(GROUP_ID.toString(), CALLER_ID.toString());

        assertThat(response.name()).isEqualTo("Trip");
        assertThat(response.members()).containsExactly(CALLER_ID.toString(), OTHER_ID.toString());
    }

    @Test
    void getGroup_throws403_whenCallerNotActiveMember() {
        when(groupMemberRepository.existsByGroup_IdAndUser_IdAndActiveTrue(GROUP_ID, CALLER_ID)).thenReturn(false);

        assertThatThrownBy(() -> groupService.getGroup(GROUP_ID.toString(), CALLER_ID.toString()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(groupRepository, never()).findById(any());
    }

    @Test
    void addMember_createsActiveMembership_whenCallerIsOwner() {
        User owner = user(CALLER_ID, "alice");
        User bob = user(OTHER_ID, "bob");
        Group g = group(GROUP_ID, "Trip", owner);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(g));
        when(userRepository.findById(OTHER_ID)).thenReturn(Optional.of(bob));
        when(groupMemberRepository.findByGroup_IdAndUser_Id(GROUP_ID, OTHER_ID)).thenReturn(Optional.empty());

        groupService.addMember(GROUP_ID.toString(), CALLER_ID.toString(), OTHER_ID.toString());

        ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(bob);
        assertThat(captor.getValue().getRole()).isEqualTo("MEMBER");
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void addMember_reactivatesExistingMembership() {
        User owner = user(CALLER_ID, "alice");
        User bob = user(OTHER_ID, "bob");
        Group g = group(GROUP_ID, "Trip", owner);
        GroupMember existing = member(g, bob, false);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(g));
        when(userRepository.findById(OTHER_ID)).thenReturn(Optional.of(bob));
        when(groupMemberRepository.findByGroup_IdAndUser_Id(GROUP_ID, OTHER_ID)).thenReturn(Optional.of(existing));

        groupService.addMember(GROUP_ID.toString(), CALLER_ID.toString(), OTHER_ID.toString());

        assertThat(existing.isActive()).isTrue();
        verify(groupMemberRepository).save(existing);
    }

    @Test
    void addMember_throws404_whenTargetUserMissing() {
        Group g = group(GROUP_ID, "Trip", user(CALLER_ID, "alice"));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(g));
        when(userRepository.findById(OTHER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.addMember(GROUP_ID.toString(), CALLER_ID.toString(), OTHER_ID.toString()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void addMember_throws403_whenCallerNotOwner() {
        Group g = group(GROUP_ID, "Trip", user(OTHER_ID, "owner"));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(g));

        assertThatThrownBy(() -> groupService.addMember(GROUP_ID.toString(), CALLER_ID.toString(), OTHER_ID.toString()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void deactivateMember_setsActiveFalse_whenCallerIsOwner() {
        User bob = user(OTHER_ID, "bob");
        Group g = group(GROUP_ID, "Trip", user(CALLER_ID, "alice"));
        GroupMember existing = member(g, bob, true);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(g));
        when(groupMemberRepository.findByGroup_IdAndUser_Id(GROUP_ID, OTHER_ID)).thenReturn(Optional.of(existing));

        groupService.deactivateMember(GROUP_ID.toString(), CALLER_ID.toString(), OTHER_ID.toString());

        assertThat(existing.isActive()).isFalse();
        verify(groupMemberRepository).save(existing);
    }

    @Test
    void deactivateMember_throws403_whenCallerNotOwner() {
        Group g = group(GROUP_ID, "Trip", user(OTHER_ID, "owner"));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(g));

        assertThatThrownBy(() -> groupService.deactivateMember(GROUP_ID.toString(), CALLER_ID.toString(), OTHER_ID.toString()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void deactivateMember_throwsWhenOwnerRemovesSelf() {
        User owner = user(CALLER_ID, "alice");
        Group g = group(GROUP_ID, "Trip", owner);
        GroupMember ownerMember = GroupMember.builder()
                .id(UUID.randomUUID()).group(g).user(owner).role("OWNER").active(true).build();
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(g));
        when(groupMemberRepository.findByGroup_IdAndUser_Id(GROUP_ID, CALLER_ID)).thenReturn(Optional.of(ownerMember));

        assertThatThrownBy(() -> groupService.deactivateMember(GROUP_ID.toString(), CALLER_ID.toString(), CALLER_ID.toString()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void deactivateMember_throws404_whenMembershipMissing() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group(GROUP_ID, "Trip", user(CALLER_ID, "alice"))));
        when(groupMemberRepository.findByGroup_IdAndUser_Id(GROUP_ID, OTHER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.deactivateMember(GROUP_ID.toString(), CALLER_ID.toString(), OTHER_ID.toString()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteGroup_disbands_whenOwner() {
        Group g = group(GROUP_ID, "Trip", user(CALLER_ID, "alice"));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(g));

        groupService.deleteGroup(GROUP_ID.toString(), CALLER_ID.toString());

        assertThat(g.isActive()).isFalse();
        verify(groupRepository).save(g);
    }

    @Test
    void deleteGroup_throws403_whenNotOwner() {
        Group g = group(GROUP_ID, "Trip", user(OTHER_ID, "owner"));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(g));

        assertThatThrownBy(() -> groupService.deleteGroup(GROUP_ID.toString(), CALLER_ID.toString()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(groupRepository, never()).save(any());
    }
}
