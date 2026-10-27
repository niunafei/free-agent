package com.webmodel.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModelProfileRepository extends JpaRepository<ModelProfile, Long> {
    List<ModelProfile> findByUserId(Long userId);
    boolean existsByUserIdAndProviderAndAccountAlias(Long userId, String provider, String accountAlias);
}