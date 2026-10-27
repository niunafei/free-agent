package com.webmodel.agent.agent.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webmodel.agent.common.BusinessException;
import com.webmodel.agent.common.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PlanParser {

    private static final Pattern FENCE = Pattern.compile("^```(?:json)?\\s*\\n(.*?)\\n```\\s*$",
            Pattern.DOTALL | Pattern.MULTILINE);

    private final ObjectMapper objectMapper;

    public PlanParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AgentPlan parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCode.TASK_PLAN_INVALID, "空响应");
        }
        String json = stripFence(raw.trim());
        try {
            AgentPlan plan = objectMapper.readValue(json, AgentPlan.class);
            validate(plan);
            return plan;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TASK_PLAN_INVALID, "JSON解析失败: " + e.getMessage());
        }
    }

    private String stripFence(String text) {
        var m = FENCE.matcher(text);
        if (m.matches()) {
            return m.group(1);
        }
        return text;
    }

    private void validate(AgentPlan plan) {
        if (plan.schemaVersion() == null || plan.schemaVersion().isBlank()) {
            throw new BusinessException(ErrorCode.TASK_PLAN_INVALID, "缺少schemaVersion");
        }
        if (plan.steps() == null || plan.steps().isEmpty()) {
            throw new BusinessException(ErrorCode.TASK_PLAN_INVALID, "步骤为空");
        }
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (PlanStep s : plan.steps()) {
            if (s.stepId() == null || !ids.add(s.stepId())) {
                throw new BusinessException(ErrorCode.TASK_PLAN_INVALID, "stepId缺失或重复: " + s.stepId());
            }
            if (s.actionType() == null) {
                throw new BusinessException(ErrorCode.TASK_PLAN_INVALID, "actionType缺失: " + s.stepId());
            }
        }
        detectCycle(plan);
    }

    private void detectCycle(AgentPlan plan) {
        java.util.Map<String, PlanStep> byId = new java.util.HashMap<>();
        for (PlanStep s : plan.steps()) byId.put(s.stepId(), s);
        java.util.Set<String> visiting = new java.util.HashSet<>();
        java.util.Set<String> visited = new java.util.HashSet<>();
        for (PlanStep s : plan.steps()) {
            dfs(s.stepId(), byId, visiting, visited);
        }
    }

    private void dfs(String id, java.util.Map<String, PlanStep> byId,
                     java.util.Set<String> visiting, java.util.Set<String> visited) {
        if (visited.contains(id)) return;
        if (!visiting.add(id)) {
            throw new BusinessException(ErrorCode.TASK_PLAN_INVALID, "检测到依赖环: " + id);
        }
        PlanStep s = byId.get(id);
        if (s != null && s.dependsOn() != null) {
            for (String dep : s.dependsOn()) {
                if (!byId.containsKey(dep)) {
                    throw new BusinessException(ErrorCode.TASK_PLAN_INVALID, "未知依赖: " + dep);
                }
                dfs(dep, byId, visiting, visited);
            }
        }
        visiting.remove(id);
        visited.add(id);
    }
}