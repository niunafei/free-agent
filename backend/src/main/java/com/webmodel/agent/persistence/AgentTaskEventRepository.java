package com.webmodel.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AgentTaskEventRepository extends JpaRepository<AgentTaskEvent, Long> {
    List<AgentTaskEvent> findByTaskIdAndSeqGreaterThanOrderBySeqAsc(Long taskId, Long lastSeq);
    List<AgentTaskEvent> findByTaskIdOrderBySeqAsc(Long taskId);

    @Query("SELECT COALESCE(MAX(e.seq), 0) FROM AgentTaskEvent e WHERE e.taskId = :taskId")
    Long findMaxSeqByTaskId(Long taskId);
}