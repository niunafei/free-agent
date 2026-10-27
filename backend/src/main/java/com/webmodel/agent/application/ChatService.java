package com.webmodel.agent.application;

import com.webmodel.agent.browser.BrowserSessionManager;
import com.webmodel.agent.common.BusinessException;
import com.webmodel.agent.common.ErrorCode;
import com.webmodel.agent.common.SnowflakeIdGenerator;
import com.webmodel.agent.events.EventPublisher;
import com.webmodel.agent.model.adapter.ModelAdapter;
import com.webmodel.agent.model.adapter.ModelAdapterFactory;
import com.webmodel.agent.persistence.*;
import com.webmodel.agent.security.UserContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final ConversationRepository conversationRepo;
    private final ConversationMessageRepository messageRepo;
    private final ModelProfileRepository modelRepo;
    private final EventPublisher eventPublisher;
    private final BrowserSessionManager browserSessionManager;
    private final ModelAdapterFactory adapterFactory;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final ToolExecutor toolExecutor;

    public ChatService(ConversationRepository conversationRepo,
                       ConversationMessageRepository messageRepo,
                       ModelProfileRepository modelRepo,
                       EventPublisher eventPublisher,
                       BrowserSessionManager browserSessionManager,
                       ModelAdapterFactory adapterFactory,
                       SnowflakeIdGenerator snowflakeIdGenerator,
                       ToolExecutor toolExecutor) {
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.modelRepo = modelRepo;
        this.eventPublisher = eventPublisher;
        this.browserSessionManager = browserSessionManager;
        this.adapterFactory = adapterFactory;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.toolExecutor = toolExecutor;
    }

    public List<Conversation> listConversations() {
        return conversationRepo.findByUserIdOrderByUpdatedAtDesc(UserContext.getUserId());
    }

    public Conversation createConversation(String title, Long modelId, String workingDir) {
        Conversation c = new Conversation();
        c.setUserId(UserContext.getUserId());
        c.setTitle(title != null ? title : "新对话");
        c.setSelectedModelId(modelId);
        c.setWorkingDir(workingDir);
        c.setContextBudget(32000);
        c.setContextStrategy("SUMMARIZE_OLD");
        c.setKeepRecentTurn(6);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return conversationRepo.save(c);
    }

    public Conversation getConversation(Long id) {
        Conversation c = conversationRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "对话不存在"));
        if (!c.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return c;
    }

    public List<ConversationMessage> messages(Long conversationId) {
        getConversation(conversationId);
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    public ConversationMessage sendMessage(Long conversationId, String content, Long modelId) {
        Conversation c = getConversation(conversationId);

        if (modelId != null && (c.getSelectedModelId() == null || !modelId.equals(c.getSelectedModelId()))) {
            c.setSelectedModelId(modelId);
            conversationRepo.save(c);
        }

        ConversationMessage userMsg = new ConversationMessage();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("USER");
        userMsg.setContent(content);
        userMsg.setCharCount(content.length());
        userMsg.setTokenEstimate(estimateTokens(content));
        userMsg.setPinned(false);
        userMsg.setSummarized(false);
        userMsg.setStatus("SUCCESS");
        userMsg.setCreatedAt(LocalDateTime.now());
        messageRepo.save(userMsg);

        c.setUpdatedAt(LocalDateTime.now());
        conversationRepo.save(c);

        eventPublisher.publish(snowflakeIdGenerator.nextId(), "USER_MESSAGE", "INFO", "用户发送消息", null);

        processModelReplyAsync(conversationId, c.getSelectedModelId(), c.getWorkingDir());

        return userMsg;
    }

    @Async("agentExecutor")
    public void processModelReplyAsync(Long conversationId, Long modelId, String workingDir) {
        if (modelId == null) {
            saveErrorMsg(conversationId, "未选择模型，请在下方下拉框选择一个已登录的模型。");
            return;
        }
        ModelProfile model = modelRepo.findById(modelId).orElse(null);
        if (model == null) {
            saveErrorMsg(conversationId, "模型不存在。");
            return;
        }
        if (!"READY".equals(model.getStatus())) {
            saveErrorMsg(conversationId, "模型未就绪，当前状态: " + model.getStatus() + "，请先完成网页登录。");
            return;
        }
        if (!browserSessionManager.hasSession(model.getId())) {
            model.setStatus("NOT_CONFIGURED");
            modelRepo.save(model);
            saveErrorMsg(conversationId, "浏览器会话已断开（后端可能重启过），请重新点击登录。");
            return;
        }

        ModelAdapter adapter = adapterFactory.getAdapter(model.getProvider());
        if (adapter == null) {
            saveErrorMsg(conversationId, "不支持的平台: " + model.getProvider());
            return;
        }

        try {
            BrowserSessionManager.BrowserSession session = browserSessionManager.getSession(model.getId());
            List<ConversationMessage> history = messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
            String contextContent = buildContext(history, workingDir);
            ModelAdapter.SendMessageResult result = adapter.sendMessage(session.page, contextContent);

            ConversationMessage modelMsg = new ConversationMessage();
            modelMsg.setConversationId(conversationId);
            modelMsg.setRole("MODEL");
            if (result.success() && result.reply() != null) {
                modelMsg.setContent(result.reply());
                modelMsg.setCharCount(result.reply().length());
                modelMsg.setTokenEstimate(estimateTokens(result.reply()));
                modelMsg.setStatus("SUCCESS");
            } else {
                modelMsg.setContent("模型回复失败: " + result.detail());
                modelMsg.setCharCount(0);
                modelMsg.setTokenEstimate(0);
                modelMsg.setStatus("ERROR");
            }
            modelMsg.setPinned(false);
            modelMsg.setSummarized(false);
            modelMsg.setCreatedAt(LocalDateTime.now());
            messageRepo.save(modelMsg);

            eventPublisher.publish(snowflakeIdGenerator.nextId(), "MODEL_REPLY", "INFO", "模型回复完成", null);

            if (result.success() && result.reply() != null && toolExecutor.hasToolCalls(result.reply())) {
                executeToolsAndReply(conversationId, result.reply(), workingDir, model, adapter, session);
            }
        } catch (Exception e) {
            saveErrorMsg(conversationId, "调用网页模型异常: " + e.getMessage());
        }
    }

    private void executeToolsAndReply(Long conversationId, String modelReply, String workingDir,
                                       ModelProfile model, ModelAdapter adapter,
                                       BrowserSessionManager.BrowserSession session) {
        List<ToolExecutor.ToolResult> toolResults = toolExecutor.executeToolsInReply(modelReply, workingDir);
        for (ToolExecutor.ToolResult tr : toolResults) {
            ConversationMessage toolMsg = new ConversationMessage();
            toolMsg.setConversationId(conversationId);
            toolMsg.setRole("TOOL");
            toolMsg.setContent("[" + tr.toolName() + "] " + (tr.success() ? "✓" : "✗") + "\n" + tr.output());
            toolMsg.setCharCount(tr.output().length());
            toolMsg.setTokenEstimate(estimateTokens(tr.output()));
            toolMsg.setPinned(false);
            toolMsg.setSummarized(false);
            toolMsg.setSourceType(tr.toolName());
            toolMsg.setStatus(tr.success() ? "SUCCESS" : "ERROR");
            toolMsg.setCreatedAt(LocalDateTime.now());
            messageRepo.save(toolMsg);
        }

        if (!toolResults.isEmpty()) {
            try {
                StringBuilder toolFeedback = new StringBuilder("工具执行结果:\n");
                for (ToolExecutor.ToolResult tr : toolResults) {
                    toolFeedback.append("[").append(tr.toolName()).append("] ")
                            .append(tr.success() ? "成功" : "失败").append(": ")
                            .append(tr.output()).append("\n\n");
                }
                toolFeedback.append("请根据以上工具执行结果继续回复。");
                ModelAdapter.SendMessageResult followUp = adapter.sendMessage(session.page, toolFeedback.toString());
                if (followUp.success() && followUp.reply() != null) {
                    ConversationMessage followUpMsg = new ConversationMessage();
                    followUpMsg.setConversationId(conversationId);
                    followUpMsg.setRole("MODEL");
                    followUpMsg.setContent(followUp.reply());
                    followUpMsg.setCharCount(followUp.reply().length());
                    followUpMsg.setTokenEstimate(estimateTokens(followUp.reply()));
                    followUpMsg.setPinned(false);
                    followUpMsg.setSummarized(false);
                    followUpMsg.setStatus("SUCCESS");
                    followUpMsg.setCreatedAt(LocalDateTime.now());
                    messageRepo.save(followUpMsg);
                }
            } catch (Exception ignored) {}
        }
    }

    private String buildContext(List<ConversationMessage> history, String workingDir) {
        StringBuilder sb = new StringBuilder();

        sb.append("【系统上下文】\n");
        if (workingDir != null && !workingDir.isBlank()) {
            sb.append("当前工作目录: ").append(workingDir).append("\n");
            sb.append("你可以使用以下工具操作文件系统:\n");
            sb.append("- 执行命令: ```bash\\n<命令>\\n```\n");
            sb.append("- 读取文件: ```file:read <文件路径>\\n```\n");
            sb.append("- 写入文件: ```file:write <文件路径>\\n<内容>\\n```\n");
            sb.append("所有文件操作均在工作目录下执行。如需查看当前目录，请执行 ```bash\\npwd\\n```。\n");
        } else {
            sb.append("未设置工作目录。如需操作文件，请先设置工作目录。\n");
        }
        sb.append("\n【对话历史】\n");

        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            ConversationMessage m = history.get(i);
            String role = "USER".equals(m.getRole()) ? "用户" : "MODEL".equals(m.getRole()) ? "模型" : "工具";
            sb.append(role).append(": ").append(m.getContent()).append("\n\n");
        }
        return sb.toString();
    }


    public Conversation renameConversation(Long id, String title, Long modelId, String workingDir) {
        Conversation c = getConversation(id);
        if (title != null) c.setTitle(title);
        if (modelId != null) c.setSelectedModelId(modelId);
        if (workingDir != null) c.setWorkingDir(workingDir);
        c.setUpdatedAt(LocalDateTime.now());
        return conversationRepo.save(c);
    }

    public void deleteConversation(Long id) {
        Conversation c = getConversation(id);
        messageRepo.deleteAll(messageRepo.findByConversationIdOrderByCreatedAtAsc(id));
        conversationRepo.delete(c);
    }

    public Map<String, Object> contextStats(Long conversationId) {
        Conversation c = getConversation(conversationId);
        List<ConversationMessage> msgs = messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
        int totalChars = msgs.stream().mapToInt(m -> m.getCharCount() != null ? m.getCharCount() : 0).sum();
        int totalTokens = msgs.stream().mapToInt(m -> m.getTokenEstimate() != null ? m.getTokenEstimate() : 0).sum();
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("messageCount", msgs.size());
        stats.put("totalChars", totalChars);
        stats.put("totalTokenEstimate", totalTokens);
        stats.put("contextBudget", c.getContextBudget());
        stats.put("budgetUsage", c.getContextBudget() > 0 ? Math.round((double) totalTokens / c.getContextBudget() * 10000) / 10000.0 : 0);
        return stats;
    }

    private ConversationMessage saveErrorMsg(Long conversationId, String content) {
        ConversationMessage msg = new ConversationMessage();
        msg.setConversationId(conversationId);
        msg.setRole("MODEL");
        msg.setContent(content);
        msg.setCharCount(0);
        msg.setTokenEstimate(0);
        msg.setPinned(false);
        msg.setSummarized(false);
        msg.setStatus("ERROR");
        msg.setCreatedAt(LocalDateTime.now());
        messageRepo.save(msg);
        return msg;
    }

    private int estimateTokens(String text) {
        return (int) Math.ceil(text.length() / 3.0);
    }
}