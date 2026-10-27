package com.webmodel.agent.events;

import com.webmodel.agent.persistence.AgentTaskEvent;
import com.webmodel.agent.persistence.AgentTaskEventRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class EventPublisher {

    private final AgentTaskEventRepository eventRepo;
    private final Map<Long, AtomicLong> seqCounters = new ConcurrentHashMap<>();
    private final SseEventBus sseEventBus;

    public EventPublisher(AgentTaskEventRepository eventRepo, SseEventBus sseEventBus) {
        this.eventRepo = eventRepo;
        this.sseEventBus = sseEventBus;
    }

    public void publish(Long taskId, String eventType, String level, String message, String payloadJson) {
        long seq = seqCounters.computeIfAbsent(taskId, k -> {
            Long maxSeq = eventRepo.findMaxSeqByTaskId(taskId);
            return new AtomicLong(maxSeq != null ? maxSeq : 0);
        }).incrementAndGet();
        AgentTaskEvent entity = new AgentTaskEvent();
        entity.setTaskId(taskId);
        entity.setSeq(seq);
        entity.setEventType(eventType);
        entity.setLevel(level);
        entity.setMessage(message);
        entity.setPayloadJson(payloadJson);
        entity.setRequiresApproval(false);
        entity.setCreatedAt(LocalDateTime.now());
        eventRepo.save(entity);

        TaskEvent event = new TaskEvent(entity.getId(), taskId, seq, entity.getCreatedAt(),
                eventType, level, null, null, message, payloadJson, null, null, null, false);
        sseEventBus.send(taskId, event);
    }

    public List<TaskEvent> history(Long taskId, Long lastSeq) {
        List<AgentTaskEvent> events = (lastSeq != null && lastSeq > 0)
                ? eventRepo.findByTaskIdAndSeqGreaterThanOrderBySeqAsc(taskId, lastSeq)
                : eventRepo.findByTaskIdOrderBySeqAsc(taskId);
        return events.stream().map(e -> new TaskEvent(
                e.getId(), e.getTaskId(), e.getSeq(), e.getCreatedAt(),
                e.getEventType(), e.getLevel(), e.getStepId(), e.getStatus(),
                e.getMessage(), e.getPayloadJson(), e.getDurationMs(),
                e.getModelId(), e.getToolId(), e.getRequiresApproval()
        )).toList();
    }
}