package com.webmodel.agent.controller;

import com.webmodel.agent.common.Result;
import com.webmodel.agent.events.EventPublisher;
import com.webmodel.agent.events.SseEventBus;
import com.webmodel.agent.events.TaskEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
public class SseController {

    private final SseEventBus sseEventBus;
    private final EventPublisher eventPublisher;

    public SseController(SseEventBus sseEventBus, EventPublisher eventPublisher) {
        this.sseEventBus = sseEventBus;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping("/api/tasks/{id}/events")
    public SseEmitter taskEvents(@PathVariable Long id,
                                 @RequestParam(required = false) Long lastEventId) {
        String clientId = UUID.randomUUID().toString();
        SseEmitter emitter = sseEventBus.subscribe(id, clientId, lastEventId);
        if (lastEventId != null && lastEventId > 0) {
            List<TaskEvent> missed = eventPublisher.history(id, lastEventId);
            new Thread(() -> {
                try {
                    for (TaskEvent e : missed) {
                        emitter.send(SseEmitter.event()
                                .id(String.valueOf(e.sequence()))
                                .name(e.eventType())
                                .data(e));
                    }
                } catch (Exception ignored) {}
            }).start();
        }
        return emitter;
    }
}