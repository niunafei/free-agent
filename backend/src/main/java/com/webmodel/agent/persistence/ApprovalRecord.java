package com.webmodel.agent.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "approval_record")
public class ApprovalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long taskId;
    private String stepId;
    private Integer planVersion;
    private String actionSummary;
    private String decision;
    private String decisionBy;
    private LocalDateTime decidedAt;
}