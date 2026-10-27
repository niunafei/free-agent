package com.webmodel.agent.agent.plan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AgentPlan(
        String schemaVersion,
        String taskSummary,
        List<PlanStep> steps,
        List<String> completionCriteria,
        @JsonProperty("requiresApproval") boolean requiresApproval
) {}