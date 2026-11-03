package com.spendmart.alerts_service.repository;

import com.spendmart.schema.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findByUser_IdOrderByTriggeredAtDesc(UUID userId);

    List<Alert> findByUser_IdAndIsReadFalse(UUID userId);
}
