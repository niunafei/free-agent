package com.webmodel.agent.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "agent_task")
public class AgentTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String title;
    private Long selectedModelId;
    private Long conversationId;
    private Long skillId;
    private String status;
    private String currentStepId;
    private Integer planVersion;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorCode;
    private String errorMsg;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String resultJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}