package com.webmodel.agent.application;

import com.webmodel.agent.controller.ConversationsController;
import com.webmodel.agent.persistence.Conversation;
import com.webmodel.agent.persistence.ConversationMessage;
import com.webmodel.agent.persistence.ConversationMessageRepository;
import com.webmodel.agent.persistence.ConversationRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepo;
    private final ConversationMessageRepository messageRepo;

    public ConversationService(ConversationRepository conversationRepo,
                               ConversationMessageRepository messageRepo) {
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
    }

    public Map<String, Object> contextStats(Long id) {
        Conversation c = conversationRepo.findById(id).orElseThrow();
        List<ConversationMessage> messages = messageRepo.findByConversationIdOrderByCreatedAtAsc(id);
        int totalChars = messages.stream().mapToInt(m -> m.getCharCount() != null ? m.getCharCount() : 0).sum();
        int totalTokens = messages.stream().mapToInt(m -> m.getTokenEstimate() != null ? m.getTokenEstimate() : 0).sum();
        Map<String, Object> stats = new HashMap<>();
        stats.put("messageCount", messages.size());
        stats.put("totalChars", totalChars);
        stats.put("totalTokenEstimate", totalTokens);
        stats.put("contextBudget", c.getContextBudget());
        stats.put("contextStrategy", c.getContextStrategy());
        stats.put("keepRecentTurn", c.getKeepRecentTurn());
        double usage = c.getContextBudget() > 0 ? (double) totalTokens / c.getContextBudget() : 0;
        stats.put("budgetUsage", Math.round(usage * 10000) / 10000.0);
        return stats;
    }

    public void updateSettings(Long id, ConversationsController.ContextSettingsRequest req) {
        Conversation c = conversationRepo.findById(id).orElseThrow();
        if (req.contextBudget() != null) c.setContextBudget(req.contextBudget());
        if (req.contextStrategy() != null) c.setContextStrategy(req.contextStrategy());
        if (req.keepRecentTurn() != null) c.setKeepRecentTurn(req.keepRecentTurn());
        conversationRepo.save(c);
    }

    public Map<String, Object> summarize(Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("preview", "摘要预览（待实现）");
        result.put("status", "PREVIEW");
        return result;
    }

    public void reset(Long id) {
        List<ConversationMessage> messages = messageRepo.findByConversationIdOrderByCreatedAtAsc(id);
        messageRepo.deleteAll(messages);
    }
}