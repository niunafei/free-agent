package com.webmodel.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentTaskStepRepository extends JpaRepository<AgentTaskStep, Long> {
    List<AgentTaskStep> findByTaskIdOrderByStepId(Long taskId);
}