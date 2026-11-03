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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupService {

    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_MEMBER = "MEMBER";

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public GroupResponse createGroup(String ownerId, String name) {
        User owner = resolveUser(ownerId);
        Group group = groupRepository.save(Group.builder()
                .name(name)
                .owner(owner)
                .build());
        groupMemberRepository.save(GroupMember.builder()
                .group(group)
                .user(owner)
                .role(ROLE_OWNER)
                .active(true)
                .build());
        return new GroupResponse(
                group.getId().toString(),
                group.getName(),
                owner.getId().toString(),
                group.getCreatedAt().toInstant(ZoneOffset.UTC));
    }

    @Transactional(readOnly = true)
    public GroupListResponse listGroups(String userId) {
        List<GroupSummary> groups = groupMemberRepository.findByUser_IdAndActiveTrue(parseId(userId)).stream()
                .filter(m -> m.getGroup().isActive())
                .map(m -> new GroupSummary(m.getGroup().getId().toString(), m.getGroup().getName()))
                .toList();
        return new GroupListResponse(groups);
    }

    @Transactional(readOnly = true)
    public GroupDetailResponse getGroup(String groupId, String requestingUserId) {
        UUID gid = parseId(groupId);
        requireActiveMember(gid, requestingUserId);
        Group group = activeGroup(gid);
        List<String> members = groupMemberRepository.findByGroup_IdAndActiveTrue(gid).stream()
                .map(m -> m.getUser().getId().toString())
                .toList();
        return new GroupDetailResponse(group.getName(), members);
    }

    public void addMember(String groupId, String requestingUserId, String newUserId) {
        UUID gid = parseId(groupId);
        Group group = requireOwner(gid, requestingUserId);
        User user = resolveUser(newUserId);

        GroupMember member = groupMemberRepository.findByGroup_IdAndUser_Id(gid, user.getId())
                .orElseGet(() -> GroupMember.builder()
                        .group(group)
                        .user(user)
                        .role(ROLE_MEMBER)
                        .build());
        member.setActive(true);
        groupMemberRepository.save(member);
    }

    public void deactivateMember(String groupId, String requestingUserId, String targetUserId) {
        UUID gid = parseId(groupId);
        requireOwner(gid, requestingUserId);
        GroupMember member = groupMemberRepository.findByGroup_IdAndUser_Id(gid, parseId(targetUserId))
                .orElseThrow(() -> new EntityNotFoundException("Member not found in group: " + targetUserId));
        if (ROLE_OWNER.equals(member.getRole())) {
            throw new IllegalArgumentException("The group owner cannot be removed; delete the group instead");
        }
        member.setActive(false);
        groupMemberRepository.save(member);
    }

    public void deleteGroup(String groupId, String requestingUserId) {
        UUID gid = parseId(groupId);
        Group group = requireOwner(gid, requestingUserId);
        group.setActive(false);
        groupRepository.save(group);
    }

    private Group activeGroup(UUID gid) {
        return groupRepository.findById(gid)
                .filter(Group::isActive)
                .orElseThrow(() -> new EntityNotFoundException("Group not found: " + gid));
    }

    private Group requireOwner(UUID gid, String userId) {
        Group group = activeGroup(gid);
        if (!group.getOwner().getId().equals(parseId(userId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group owner can manage members");
        }
        return group;
    }

    private void requireActiveMember(UUID groupId, String userId) {
        if (!groupMemberRepository.existsByGroup_IdAndUser_IdAndActiveTrue(groupId, parseId(userId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not an active member of this group");
        }
    }

    private User resolveUser(String userId) {
        return userRepository.findById(parseId(userId))
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    private UUID parseId(String id) {
        return UUID.fromString(id);
    }
}
