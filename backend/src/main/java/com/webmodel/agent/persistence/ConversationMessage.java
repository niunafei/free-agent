package com.webmodel.agent.persistence;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "conversation_message")
public class ConversationMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long conversationId;
    private String role;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;
    private Integer charCount;
    private Integer tokenEstimate;
    private Boolean pinned;
    private Boolean summarized;
    private String sourceType;
    private String status;
    private LocalDateTime createdAt;
}