package com.webmodel.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface McpServerConfigRepository extends JpaRepository<McpServerConfig, Long> {
    List<McpServerConfig> findByUserId(Long userId);
}