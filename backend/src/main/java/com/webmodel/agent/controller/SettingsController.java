package com.webmodel.agent.controller;

import com.webmodel.agent.application.SettingsService;
import com.webmodel.agent.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public Result<Map<String, String>> get() {
        return Result.success(settingsService.getAll());
    }

    @PatchMapping("/agent")
    public Result<Void> updateAgent(@RequestBody Map<String, String> settings) {
        settingsService.updateAgent(settings);
        return Result.success();
    }
}