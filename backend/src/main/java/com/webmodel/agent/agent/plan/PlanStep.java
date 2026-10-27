package com.webmodel.agent.agent.plan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record PlanStep(
        String stepId,
        String actionType,
        String target,
        Map<String, Object> arguments,
        @JsonProperty("dependsOn") List<String> dependsOn,
        String riskLevel,
        String expectedOutput
) {}