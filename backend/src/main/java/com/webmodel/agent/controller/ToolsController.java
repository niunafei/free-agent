package com.webmodel.agent.controller;

import com.webmodel.agent.application.ToolService;
import com.webmodel.agent.common.Result;
import com.webmodel.agent.persistence.ToolDefinition;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolsController {

    private final ToolService toolService;

    public ToolsController(ToolService toolService) {
        this.toolService = toolService;
    }

    @GetMapping
    public Result<List<ToolDefinition>> list() {
        return Result.success(toolService.listAll());
    }

    @PatchMapping("/{id}")
    public Result<ToolDefinition> update(@PathVariable Long id, @RequestBody ToolUpdateRequest req) {
        return Result.success(toolService.update(id, req));
    }

    public record ToolUpdateRequest(Boolean enabled, Long timeoutMs) {}
}