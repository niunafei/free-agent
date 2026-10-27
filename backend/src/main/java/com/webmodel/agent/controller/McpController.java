package com.webmodel.agent.controller;

import com.webmodel.agent.application.McpService;
import com.webmodel.agent.common.Result;
import com.webmodel.agent.persistence.McpCapability;
import com.webmodel.agent.persistence.McpServerConfig;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mcp/servers")
public class McpController {

    private final McpService mcpService;

    public McpController(McpService mcpService) {
        this.mcpService = mcpService;
    }

    @GetMapping
    public Result<List<McpServerConfig>> list() {
        return Result.success(mcpService.listServers());
    }

    @PostMapping
    public Result<McpServerConfig> create(@Valid @RequestBody McpCreateRequest req) {
        return Result.success(mcpService.create(req));
    }

    @PostMapping("/{id}/connect")
    public Result<Void> connect(@PathVariable Long id) {
        mcpService.connect(id);
        return Result.success();
    }

    @GetMapping("/{id}/capabilities")
    public Result<List<McpCapability>> capabilities(@PathVariable Long id) {
        return Result.success(mcpService.capabilities(id));
    }

    @PostMapping("/{id}/disconnect")
    public Result<Void> disconnect(@PathVariable Long id) {
        mcpService.disconnect(id);
        return Result.success();
    }

    public record McpCreateRequest(
            String name,
            String transportType,
            String endpointOrCommandRef,
            String trustLevel) {}
}