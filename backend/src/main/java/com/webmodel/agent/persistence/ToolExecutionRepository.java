package com.webmodel.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToolExecutionRepository extends JpaRepository<ToolExecution, Long> {
    List<ToolExecution> findByTaskId(Long taskId);
}