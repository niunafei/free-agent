package com.webmodel.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ToolDefinitionRepository extends JpaRepository<ToolDefinition, Long> {
    List<ToolDefinition> findByEnabledTrue();
    Optional<ToolDefinition> findByToolId(String toolId);
}