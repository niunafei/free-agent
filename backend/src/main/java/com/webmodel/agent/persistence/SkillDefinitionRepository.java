package com.webmodel.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillDefinitionRepository extends JpaRepository<SkillDefinition, Long> {
    List<SkillDefinition> findByUserId(Long userId);
}