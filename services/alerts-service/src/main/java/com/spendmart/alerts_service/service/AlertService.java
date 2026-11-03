package com.spendmart.alerts_service.service;

import com.spendmart.alerts_service.dto.AlertResponse;
import com.spendmart.alerts_service.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    public void createAlert(String userId, String type, String message) {
        // TODO: create Alert entity with triggeredAt = now, isRead = false, save
    }

    public List<AlertResponse> getAlerts(String userId) {
        // TODO: findByUserIdOrderByTriggeredAtDesc, map to AlertResponse
        return null;
    }

    public List<AlertResponse> getUnreadAlerts(String userId) {
        // TODO: findByUserIdAndIsReadFalse, map to AlertResponse
        return null;
    }

    public void markAsRead(String alertId, String userId) {
        // TODO: find alert, verify owner, set isRead = true, save
    }
}
