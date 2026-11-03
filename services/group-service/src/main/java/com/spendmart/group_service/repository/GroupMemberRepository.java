package com.spendmart.group_service.repository;

import com.spendmart.schema.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    List<GroupMember> findByGroup_IdAndActiveTrue(UUID groupId);

    List<GroupMember> findByUser_IdAndActiveTrue(UUID userId);

    boolean existsByGroup_IdAndUser_IdAndActiveTrue(UUID groupId, UUID userId);

    Optional<GroupMember> findByGroup_IdAndUser_Id(UUID groupId, UUID userId);
}
