package com.webmodel.agent.persistence;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "mcp_capability")
public class McpCapability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long serverId;
    private String capabilityType;
    private String capabilityName;
    private String description;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String schemaJson;
    private Boolean enabled;
    private String riskLevel;
}