package com.webmodel.agent.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "skill_definition")
public class SkillDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String skillId;
    private String name;
    private String version;
    private String description;
    private String sourcePath;
    private String contentHash;
    private Boolean enabled;
    private String validationStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}