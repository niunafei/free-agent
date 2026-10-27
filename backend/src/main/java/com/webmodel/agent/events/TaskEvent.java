package com.webmodel.agent.events;

import java.time.LocalDateTime;

public record TaskEvent(
        Long eventId,
        Long taskId,
        Long sequence,
        LocalDateTime timestamp,
        String eventType,
        String level,
        String stepId,
        String status,
        String message,
        String payloadJson,
        Long durationMs,
        Long modelId,
        String toolId,
        Boolean requiresApproval
) {}