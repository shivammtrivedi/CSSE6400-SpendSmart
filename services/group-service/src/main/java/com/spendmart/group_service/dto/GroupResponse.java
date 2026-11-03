package com.spendmart.group_service.dto;

import java.time.Instant;

public record GroupResponse(String groupId, String name, String createdBy, Instant createdAt) {}
