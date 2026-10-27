package com.webmodel.agent.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "agent_plan")
public class AgentPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long taskId;
    private Integer version;
    private String schemaVersion;
    private String rawResponseRef;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String validatedJson;
    private String validationStatus;
    private LocalDateTime createdAt;
}