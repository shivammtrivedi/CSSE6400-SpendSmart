package com.spendmart.expense_service.service;

import com.spendmart.expense_service.dto.GroupStreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseSubscriptionManager {
    private static final Logger log = LoggerFactory.getLogger(SseSubscriptionManager.class);

    // SSE emission is mapped to group, as changes are scoped to group visibility by design
    private final Map<UUID, Map<String, SseEmitter>> emittersByGroup = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID groupId) {
        SseEmitter emitter = new SseEmitter(30L * 60L * 1000L); // Timeout of 30 minutes
        String subscriptionId = UUID.randomUUID().toString();

        emittersByGroup
                .computeIfAbsent(groupId, ignored -> new ConcurrentHashMap<>())
                .put(subscriptionId, emitter);

        log.info(
                "SSE emitter added: groupId={}, subscriptionId={}, groupSubscribers={}, totalSubscribers={}",
                groupId,
                subscriptionId,
                groupSubscriberCount(groupId),
                totalSubscriberCount()
        );

        emitter.onCompletion(() -> remove(groupId, subscriptionId));
        emitter.onTimeout(() -> remove(groupId, subscriptionId));
        emitter.onError(ignored -> remove(groupId, subscriptionId));

        try {
            emitter.send(SseEmitter.event()
                    .id(subscriptionId)
                    .name("CONNECTED")
                    .data("connected"));
        } catch (IOException ex) {
            remove(groupId, subscriptionId);
        }

        return emitter;
    }

    public void publish(UUID groupId, GroupStreamEvent event) {
        Map<String, SseEmitter> groupEmitters = emittersByGroup.get(groupId);
        if (groupEmitters == null || groupEmitters.isEmpty()) {
            log.debug("SSE publish skipped (no subscribers): groupId={}, event={}", groupId, event.eventName());
            return;
        }

        log.info(
                "SSE publish: groupId={}, event={}, subscribers={}, totalSubscribers={}",
                groupId,
                event.eventName(),
                groupEmitters.size(),
                totalSubscriberCount()
        );

        for (Map.Entry<String, SseEmitter> entry : new ArrayList<>(groupEmitters.entrySet())) {
            String subscriptionId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            try {
                emitter.send(SseEmitter.event()
                        .id(UUID.randomUUID().toString())
                        .name(event.eventName())
                        .data(event));
            } catch (IOException ex) {
                remove(groupId, subscriptionId);
            }
        }
    }

    private void remove(UUID groupId, String subscriptionId) {
        Map<String, SseEmitter> groupEmitters = emittersByGroup.get(groupId);
        if (groupEmitters == null) {
            return;
        }
        groupEmitters.remove(subscriptionId);
        int remainingInGroup = groupEmitters.size();
        if (groupEmitters.isEmpty()) {
            emittersByGroup.remove(groupId);
            remainingInGroup = 0;
        }

        log.info(
                "SSE emitter removed: groupId={}, subscriptionId={}, groupSubscribers={}, totalSubscribers={}",
                groupId,
                subscriptionId,
                remainingInGroup,
                totalSubscriberCount()
        );
    }

    private int groupSubscriberCount(UUID groupId) {
        Map<String, SseEmitter> groupEmitters = emittersByGroup.get(groupId);
        return groupEmitters == null ? 0 : groupEmitters.size();
    }

    private int totalSubscriberCount() {
        return emittersByGroup.values().stream().mapToInt(Map::size).sum();
    }
}
