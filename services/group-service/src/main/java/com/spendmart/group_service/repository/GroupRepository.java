package com.spendmart.group_service.repository;

import com.spendmart.schema.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    List<Group> findByOwner_Id(UUID ownerId);
}
