package com.webmodel.agent.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tool_execution")
public class ToolExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long taskId;
    private String stepId;
    private String toolId;
    private String argsSummary;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String resultSummary;
    private String status;
    private Boolean requiresApproval;
    private LocalDateTime approvedAt;
    private Long durationMs;
    private LocalDateTime createdAt;
}