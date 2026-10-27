package com.webmodel.agent.controller;

import com.webmodel.agent.application.ChatService;
import com.webmodel.agent.common.Result;
import com.webmodel.agent.persistence.Conversation;
import com.webmodel.agent.persistence.ConversationMessage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    public Result<List<Conversation>> conversations() {
        return Result.success(chatService.listConversations());
    }

    @PostMapping("/conversations")
    public Result<Conversation> createConversation(@RequestBody CreateConversationRequest req) {
        return Result.success(chatService.createConversation(req.title(), req.modelId(), req.workingDir()));
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<List<ConversationMessage>> messages(@PathVariable Long id) {
        return Result.success(chatService.messages(id));
    }

    @PostMapping("/conversations/{id}/messages")
    public Result<ConversationMessage> sendMessage(@PathVariable Long id, @Valid @RequestBody SendMessageRequest req) {
        return Result.success(chatService.sendMessage(id, req.content(), req.modelId()));
    }

    @PatchMapping("/conversations/{id}")
    public Result<Conversation> renameConversation(@PathVariable Long id, @RequestBody RenameConversationRequest req) {
        return Result.success(chatService.renameConversation(id, req.title(), req.modelId(), req.workingDir()));
    }

    @DeleteMapping("/conversations/{id}")
    public Result<Void> deleteConversation(@PathVariable Long id) {
        chatService.deleteConversation(id);
        return Result.success(null);
    }

    @GetMapping("/conversations/{id}/context")
    public Result<Map<String, Object>> context(@PathVariable Long id) {
        return Result.success(chatService.contextStats(id));
    }

    public record CreateConversationRequest(String title, Long modelId, String workingDir) {}
    public record SendMessageRequest(String content, Long modelId) {}
    public record RenameConversationRequest(String title, Long modelId, String workingDir) {}
}