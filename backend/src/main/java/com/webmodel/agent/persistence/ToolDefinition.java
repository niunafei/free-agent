package com.webmodel.agent.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tool_definition")
public class ToolDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String toolId;
    private String name;
    private String description;
    private String category;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String inputSchemaJson;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String outputSchemaJson;
    private String riskLevel;
    private Boolean enabled;
    private Boolean isSystem;
    private Long timeoutMs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}