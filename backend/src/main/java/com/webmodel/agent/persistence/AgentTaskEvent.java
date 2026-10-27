package com.webmodel.agent.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "agent_task_event")
public class AgentTaskEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long taskId;
    private Long seq;
    private String eventType;
    private String level;
    private String stepId;
    private String status;
    private String message;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String payloadJson;
    private Long durationMs;
    private Long modelId;
    private String toolId;
    private Boolean requiresApproval;
    private LocalDateTime createdAt;
}