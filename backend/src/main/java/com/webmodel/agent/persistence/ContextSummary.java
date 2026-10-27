package com.webmodel.agent.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "context_summary")
public class ContextSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long conversationId;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String summaryText;
    private Long coveredFrom;
    private Long coveredTo;
    private Integer version;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}