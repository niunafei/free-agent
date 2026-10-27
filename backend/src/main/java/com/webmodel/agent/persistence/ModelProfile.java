package com.webmodel.agent.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "model_profile")
public class ModelProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String provider;
    private String displayName;
    private String accountAlias;
    private String adapterType;
    private String baseUrl;
    private String profileKey;
    private Boolean enabled;
    private String status;
    private String loginSessionId;
    private LocalDateTime lastCheckedAt;
    private String lastErrorCode;
    private String lastErrorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}