package com.test.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class SseEmitterRegistry {

    private static final long TIMEOUT_MS = 30L * 60 * 1000;

    private final Map<String, List<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        emittersByUserId.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("SSE subscribed: userId={}, total connections for this user={}", userId, emittersByUserId.get(userId).size());

        emitter.onCompletion(() -> {
            log.info("SSE completed: userId={}", userId);
            removeEmitter(userId, emitter);
        });
        emitter.onTimeout(() -> {
            log.info("SSE timed out: userId={}", userId);
            removeEmitter(userId, emitter);
        });
        emitter.onError(e -> {
            log.warn("SSE error: userId={}, error={}", userId, e.getMessage());
            removeEmitter(userId, emitter);
        });

        return emitter;
    }

    public void sendToUser(String userId, Object payload) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);

        if (emitters == null || emitters.isEmpty()) {
            log.warn("SSE push skipped — no active connection for userId={}", userId);
            return;
        }

        log.info("SSE pushing to userId={}, {} connection(s)", userId, emitters.size());

        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));
                log.info("SSE push succeeded for userId={}", userId);
            } catch (IOException e) {
                log.warn("SSE push failed for userId={}: {}", userId, e.getMessage());
                removeEmitter(userId, emitter);
            }
        }
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByUserId.remove(userId);
            }
        }
    }
}