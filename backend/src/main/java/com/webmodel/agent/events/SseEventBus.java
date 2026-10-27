package com.webmodel.agent.events;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEventBus {

    private final Map<Long, Map<String, SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long taskId, String clientId, Long lastEventId) {
        SseEmitter emitter = new SseEmitter(0L);
        subscribers.computeIfAbsent(taskId, k -> new ConcurrentHashMap<>()).put(clientId, emitter);
        emitter.onCompletion(() -> remove(taskId, clientId));
        emitter.onTimeout(() -> remove(taskId, clientId));
        emitter.onError(e -> remove(taskId, clientId));
        return emitter;
    }

    public void send(Long taskId, TaskEvent event) {
        Map<String, SseEmitter> map = subscribers.get(taskId);
        if (map == null || map.isEmpty()) return;
        map.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(event.sequence()))
                        .name(event.eventType())
                        .data(event));
            } catch (IOException e) {
                remove(taskId, clientId);
            }
        });
    }

    private void remove(Long taskId, String clientId) {
        Map<String, SseEmitter> map = subscribers.get(taskId);
        if (map != null) map.remove(clientId);
    }
}