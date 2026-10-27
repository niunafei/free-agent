package com.webmodel.agent.controller;

import com.webmodel.agent.application.SkillService;
import com.webmodel.agent.common.Result;
import com.webmodel.agent.persistence.SkillDefinition;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillsController {

    private final SkillService skillService;

    public SkillsController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public Result<List<SkillDefinition>> list() {
        return Result.success(skillService.listAll());
    }

    @PostMapping("/import")
    public Result<SkillDefinition> importSkill(@RequestBody SkillImportRequest req) {
        return Result.success(skillService.importSkill(req));
    }

    @PatchMapping("/{id}")
    public Result<SkillDefinition> update(@PathVariable Long id, @RequestBody SkillUpdateRequest req) {
        return Result.success(skillService.update(id, req));
    }

    public record SkillImportRequest(String sourcePath) {}

    public record SkillUpdateRequest(Boolean enabled) {}
}