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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ConversationRepository conversationRepo;
    private final ConversationMessageRepository messageRepo;
    private final ModelProfileRepository modelRepo;
    private final EventPublisher eventPublisher;
    private final BrowserSessionManager browserSessionManager;
    private final ModelAdapterFactory adapterFactory;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final ToolExecutor toolExecutor;
    private final ToolDefinitionRepository toolDefRepo;
    private final McpServerConfigRepository mcpServerRepo;
    private final McpCapabilityRepository mcpCapabilityRepo;
    private final SkillDefinitionRepository skillDefRepo;

    public ChatService(ConversationRepository conversationRepo,
                       ConversationMessageRepository messageRepo,
                       ModelProfileRepository modelRepo,
                       EventPublisher eventPublisher,
                       BrowserSessionManager browserSessionManager,
                       ModelAdapterFactory adapterFactory,
                       SnowflakeIdGenerator snowflakeIdGenerator,
                       ToolExecutor toolExecutor,
                       ToolDefinitionRepository toolDefRepo,
                       McpServerConfigRepository mcpServerRepo,
                       McpCapabilityRepository mcpCapabilityRepo,
                       SkillDefinitionRepository skillDefRepo) {
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.modelRepo = modelRepo;
        this.eventPublisher = eventPublisher;
        this.browserSessionManager = browserSessionManager;
        this.adapterFactory = adapterFactory;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.toolExecutor = toolExecutor;
        this.toolDefRepo = toolDefRepo;
        this.mcpServerRepo = mcpServerRepo;
        this.mcpCapabilityRepo = mcpCapabilityRepo;
        this.skillDefRepo = skillDefRepo;
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

        processModelReplyAsync(conversationId, c.getSelectedModelId(), c.getWorkingDir(), c.getUserId());

        return userMsg;
    }

    @Async("agentExecutor")
    public void processModelReplyAsync(Long conversationId, Long modelId, String workingDir, Long userId) {
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
            String contextContent = buildContext(history, workingDir, userId);
            log.info("[对话{}] 发送给模型, 内容长度={}, 内容:\n{}", conversationId, contextContent.length(), contextContent);
            ModelAdapter.SendMessageResult result = adapter.sendMessage(session.page, contextContent);
            log.info("[对话{}] 模型回复, success={}, reply:\n{}", conversationId, result.success(), result.reply());

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
        int maxRounds = 10;
        String currentReply = modelReply;

        for (int round = 1; round <= maxRounds; round++) {
            if (!toolExecutor.hasToolCalls(currentReply)) {
                break;
            }

            eventPublisher.publish(snowflakeIdGenerator.nextId(), "TOOL_ROUND", "INFO",
                    "Agent Loop 第" + round + "轮工具执行", null);

            log.info("[对话{}] 第{}轮工具调用前, 模型回复:\n{}", conversationId, round, currentReply);

            List<ToolExecutor.ToolResult> toolResults = toolExecutor.executeToolsInReply(currentReply, workingDir);
            for (ToolExecutor.ToolResult tr : toolResults) {
                log.info("[对话{}] 第{}轮工具调用后, tool={}, success={}, output:\n{}", conversationId, round, tr.toolName(), tr.success(), tr.output());
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

            if (toolResults.isEmpty()) {
                break;
            }

            try {
                StringBuilder toolFeedback = new StringBuilder();
                toolFeedback.append("[工具执行结果 - 第").append(round).append("轮]\n");
                for (ToolExecutor.ToolResult tr : toolResults) {
                    toolFeedback.append("[").append(tr.toolName()).append("] ")
                            .append(tr.success() ? "成功" : "失败").append(":\n")
                            .append(tr.output()).append("\n\n");
                }
                toolFeedback.append("以上是工具执行结果。如需继续操作请用<tool_call>格式调用工具,否则请直接给出最终回复。");

                String feedbackStr = toolFeedback.toString();
                log.info("[对话{}] 第{}轮发送工具结果给模型, 内容长度={}, 内容:\n{}", conversationId, round, feedbackStr.length(), feedbackStr);
                ModelAdapter.SendMessageResult followUp = adapter.sendMessage(session.page, feedbackStr);
                log.info("[对话{}] 第{}轮模型回复, success={}, reply:\n{}", conversationId, round, followUp.success(), followUp.reply());
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

                    currentReply = followUp.reply();
                } else {
                    saveErrorMsg(conversationId, "第" + round + "轮模型回复失败: " + followUp.detail());
                    break;
                }
            } catch (Exception e) {
                saveErrorMsg(conversationId, "第" + round + "轮执行异常: " + e.getMessage());
                break;
            }
        }

        eventPublisher.publish(snowflakeIdGenerator.nextId(), "AGENT_DONE", "INFO", "Agent Loop 完成", null);
    }

    private String buildContext(List<ConversationMessage> history, String workingDir, Long userId) {
        StringBuilder sb = new StringBuilder();

        sb.append("[系统指令] 你是一个Agent,可以调用工具完成任务。\n");
        if (workingDir != null && !workingDir.isBlank()) {
            sb.append("工作目录: ").append(workingDir).append("\n");
        }

        sb.append("\n【工具调用协议】\n");
        sb.append("当你需要执行命令或操作文件时,输出以下格式(可输出多个,按顺序执行):\n");
        sb.append("<tool_call>\n");
        sb.append("{\"tool\": \"bash\", \"args\": {\"command\": \"ls -la\"}}\n");
        sb.append("</tool_call>\n");
        sb.append("<tool_call>\n");
        sb.append("{\"tool\": \"file.read\", \"args\": {\"path\": \"src/main.java\"}}\n");
        sb.append("</tool_call>\n");
        sb.append("<tool_call>\n");
        sb.append("{\"tool\": \"file.write\", \"args\": {\"path\": \"output.txt\", \"content\": \"内容\"}}\n");
        sb.append("</tool_call>\n");
        sb.append("tool字段: bash/file.read/file.write/file.list/file.search\n");
        sb.append("args字段: bash用command, file.read用path, file.write用path+content\n\n");

        sb.append("【工具结果格式】\n");
        sb.append("工具执行后,你会收到以下格式的结果:\n");
        sb.append("<tool_result tool=\"bash\" success=\"true\">\n退出码: 0\n输出内容\n</tool_result>\n");
        sb.append("收到结果后,你可以继续调用工具或给出最终回复。\n\n");

        sb.append(buildToolSummary());
        sb.append(buildMcpSummary(userId));
        sb.append(buildSkillSummary(userId));
        sb.append("\n");

        int start = Math.max(0, history.size() - 6);
        if (start > 0) {
            sb.append("(更早历史省略)\n");
        }
        for (int i = start; i < history.size(); i++) {
            ConversationMessage m = history.get(i);
            String role = "USER".equals(m.getRole()) ? "用户" : "MODEL".equals(m.getRole()) ? "模型" : "工具";
            String content = m.getContent();
            if (content != null && content.length() > 500) {
                content = content.substring(0, 500) + "...(省略)";
            }
            sb.append(role).append(": ").append(content).append("\n\n");
        }
        return sb.toString();
    }

    private String buildToolSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("工具: ");
        sb.append("bash(执行命令), file:read(读文件), file:write(写文件)");
        try {
            List<ToolDefinition> tools = toolDefRepo.findByEnabledTrue();
            if (tools != null && !tools.isEmpty()) {
                List<String> names = new java.util.ArrayList<>();
                for (ToolDefinition t : tools) {
                    String id = t.getToolId() != null ? t.getToolId() : t.getName();
                    if (!id.equals("bash") && !id.equals("file.read") && !id.equals("file.write")) {
                        names.add(id + "(" + (t.getDescription() != null ? t.getDescription().substring(0, Math.min(20, t.getDescription().length())) : "") + ")");
                    }
                }
                if (!names.isEmpty()) {
                    sb.append(", ").append(String.join(", ", names));
                }
            }
        } catch (Exception ignored) {}
        sb.append(".\n");
        return sb.toString();
    }

    private String buildMcpSummary(Long userId) {
        StringBuilder sb = new StringBuilder();
        try {
            List<McpServerConfig> servers = mcpServerRepo.findByUserIdOrUserIdIsNull(userId);
            if (servers != null && !servers.isEmpty()) {
                List<String> connected = new java.util.ArrayList<>();
                for (McpServerConfig s : servers) {
                    if (Boolean.TRUE.equals(s.getEnabled()) && "CONNECTED".equals(s.getConnectionStatus())) {
                        connected.add(s.getName());
                    }
                }
                if (!connected.isEmpty()) {
                    sb.append("MCP: ").append(String.join(", ", connected)).append(".\n");
                }
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    private String buildSkillSummary(Long userId) {
        StringBuilder sb = new StringBuilder();
        try {
            List<SkillDefinition> skills = skillDefRepo.findByUserIdOrUserIdIsNull(userId);
            if (skills != null && !skills.isEmpty()) {
                List<String> names = new java.util.ArrayList<>();
                for (SkillDefinition s : skills) {
                    if (Boolean.TRUE.equals(s.getEnabled())) {
                        names.add(s.getName() != null ? s.getName() : s.getSkillId());
                    }
                }
                if (!names.isEmpty()) {
                    sb.append("技能: ").append(String.join(", ", names)).append(".\n");
                }
            }
        } catch (Exception ignored) {}
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