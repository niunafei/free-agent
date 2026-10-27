package com.webmodel.agent.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "prompt_template")
public class PromptTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String templateContent;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String outputSchemaJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}