package com.webmodel.agent.controller;

import com.webmodel.agent.application.ModelService;
import com.webmodel.agent.common.Result;
import com.webmodel.agent.persistence.ModelProfile;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/models")
public class ModelsController {

    private final ModelService modelService;

    public ModelsController(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping
    public Result<List<ModelProfile>> list() {
        return Result.success(modelService.listAll());
    }

    @PostMapping
    public Result<ModelProfile> create(@Valid @RequestBody ModelCreateRequest req) {
        return Result.success(modelService.create(req));
    }

    @PatchMapping("/{id}")
    public Result<ModelProfile> update(@PathVariable Long id, @RequestBody ModelUpdateRequest req) {
        return Result.success(modelService.update(id, req));
    }

    @PostMapping("/{id}/connect")
    public Result<Void> connect(@PathVariable Long id) {
        modelService.connect(id);
        return Result.success();
    }

    @PostMapping("/{id}/check")
    public Result<ModelProfile> check(@PathVariable Long id) {
        return Result.success(modelService.check(id));
    }

    @PostMapping("/{id}/disconnect")
    public Result<Void> disconnect(@PathVariable Long id) {
        modelService.disconnect(id);
        return Result.success();
    }

    @GetMapping("/{id}/login-status")
    public Result<com.webmodel.agent.application.ModelService.LoginStatusResult> loginStatus(@PathVariable Long id) {
        return Result.success(modelService.loginStatus(id));
    }

    public record ModelCreateRequest(
            String provider,
            String displayName,
            String accountAlias,
            String adapterType,
            String baseUrl) {}

    public record ModelUpdateRequest(
            String displayName,
            String baseUrl,
            Boolean enabled) {}
}