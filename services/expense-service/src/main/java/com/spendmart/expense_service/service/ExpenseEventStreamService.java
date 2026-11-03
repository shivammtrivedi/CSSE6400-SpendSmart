package com.spendmart.expense_service.service;

import com.spendmart.expense_service.exception.ForbiddenException;
import com.spendmart.expense_service.repository.GroupMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Service
public class ExpenseEventStreamService {
    private static final Logger log = LoggerFactory.getLogger(ExpenseEventStreamService.class);

    private final GroupMemberRepository groupMemberRepository;
    private final SseSubscriptionManager subscriptionManager;

    public ExpenseEventStreamService(
            GroupMemberRepository groupMemberRepository,
            SseSubscriptionManager subscriptionManager
    ) {
        this.groupMemberRepository = groupMemberRepository;
        this.subscriptionManager = subscriptionManager;
    }

    @Transactional(readOnly = true)
    public SseEmitter subscribe(UUID groupId, UUID userId) {
        log.info("SSE subscribe requested: groupId={}, userId={}", groupId, userId);
        assertActiveGroupMember(userId, groupId);
        SseEmitter emitter = subscriptionManager.subscribe(groupId);
        log.info("SSE subscribe accepted: groupId={}, userId={}", groupId, userId);
        return emitter;
    }

    private void assertActiveGroupMember(UUID userId, UUID groupId) {
        boolean activeMember = groupMemberRepository.existsByGroup_IdAndUser_IdAndActiveTrue(groupId, userId);
        if (!activeMember) {
            throw new ForbiddenException("Caller is not an active member of this group");
        }
    }
}
