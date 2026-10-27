package com.webmodel.agent.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentTaskRepository extends JpaRepository<AgentTask, Long> {
    Page<AgentTask> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<AgentTask> findByStatus(String status);
}