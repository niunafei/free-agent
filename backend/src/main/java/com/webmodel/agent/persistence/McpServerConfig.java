package com.webmodel.agent.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "mcp_server_config")
public class McpServerConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String name;
    private String transportType;
    private String endpointOrCommandRef;
    private Boolean enabled;
    private String connectionStatus;
    private String trustLevel;
    private LocalDateTime lastCheckedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}