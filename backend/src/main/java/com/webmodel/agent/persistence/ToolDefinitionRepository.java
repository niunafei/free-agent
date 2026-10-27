package com.webmodel.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToolDefinitionRepository extends JpaRepository<ToolDefinition, Long> {
    List<ToolDefinition> findByEnabledTrue();
}