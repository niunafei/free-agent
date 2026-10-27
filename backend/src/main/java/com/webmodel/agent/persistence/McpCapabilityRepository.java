package com.webmodel.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface McpCapabilityRepository extends JpaRepository<McpCapability, Long> {
    List<McpCapability> findByServerId(Long serverId);
}