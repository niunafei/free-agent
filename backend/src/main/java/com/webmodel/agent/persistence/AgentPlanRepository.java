package com.webmodel.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentPlanRepository extends JpaRepository<AgentPlan, Long> {
    List<AgentPlan> findByTaskIdOrderByVersionDesc(Long taskId);
}