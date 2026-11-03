package com.spendmart.alerts_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AlertResponse {

    private Long id;
    private String type;
    private String message;
    private LocalDateTime triggeredAt;
    private boolean isRead;
}
