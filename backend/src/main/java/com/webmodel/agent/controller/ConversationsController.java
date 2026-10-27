package com.webmodel.agent.controller;

import com.webmodel.agent.application.ConversationService;
import com.webmodel.agent.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationsController {

    private final ConversationService conversationService;

    public ConversationsController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/{id}/context")
    public Result<Map<String, Object>> context(@PathVariable Long id) {
        return Result.success(conversationService.contextStats(id));
    }

    @PatchMapping("/{id}/context/settings")
    public Result<Void> updateSettings(@PathVariable Long id, @RequestBody ContextSettingsRequest req) {
        conversationService.updateSettings(id, req);
        return Result.success();
    }

    @PostMapping("/{id}/context/summarize")
    public Result<Map<String, Object>> summarize(@PathVariable Long id) {
        return Result.success(conversationService.summarize(id));
    }

    @PostMapping("/{id}/reset")
    public Result<Void> reset(@PathVariable Long id) {
        conversationService.reset(id);
        return Result.success();
    }

    public record ContextSettingsRequest(
            Integer contextBudget,
            String contextStrategy,
            Integer keepRecentTurn) {}
}