package com.spendmart.expense_service.repository;

import com.spendmart.schema.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    boolean existsByGroup_IdAndUser_IdAndActiveTrue(UUID groupId, UUID userId);

    long countByGroup_IdAndUser_IdInAndActiveTrue(UUID groupId, Collection<UUID> userIds);
}
