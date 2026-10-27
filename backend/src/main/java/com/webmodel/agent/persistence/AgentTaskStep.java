package com.webmodel.agent.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "agent_task_step")
public class AgentTaskStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long taskId;
    private Long planId;
    private String stepId;
    private String actionType;
    private String targetId;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String argumentsJson;
    private String dependsOnJson;
    private String riskLevel;
    private String status;
    private Integer retryCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationMs;
}