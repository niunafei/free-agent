package com.webmodel.agent.application;

import com.webmodel.agent.common.BusinessException;
import com.webmodel.agent.common.ErrorCode;
import com.webmodel.agent.common.PageResult;
import com.webmodel.agent.controller.TasksController;
import com.webmodel.agent.events.EventPublisher;
import com.webmodel.agent.persistence.AgentTask;
import com.webmodel.agent.persistence.AgentTaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class TaskService {

    private static final Set<String> TERMINAL = Set.of("SUCCEEDED", "FAILED", "CANCELLED");

    private final AgentTaskRepository repository;
    private final EventPublisher eventPublisher;

    public TaskService(AgentTaskRepository repository, EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public PageResult<AgentTask> page(int page, int size) {
        Page<AgentTask> p = repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page - 1, size));
        return PageResult.of(p.getTotalElements(), page, size, p.getContent());
    }

    public AgentTask get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
    }

    public AgentTask create(TasksController.TaskCreateRequest req) {
        AgentTask t = new AgentTask();
        t.setTitle(req.title());
        t.setSelectedModelId(req.selectedModelId());
        t.setConversationId(req.conversationId());
        t.setSkillId(req.skillId());
        t.setStatus("CREATED");
        t.setPlanVersion(0);
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        t = repository.save(t);
        eventPublisher.publish(t.getId(), "TASK_CREATED", "INFO", "任务已创建", null);
        return t;
    }

    public void approve(Long id, TasksController.ApproveRequest req) {
        AgentTask t = get(id);
        eventPublisher.publish(t.getId(), "APPROVAL_DECISION", "INFO",
                "审批决定: " + req.decision(), null);
    }

    public void pause(Long id) {
        AgentTask t = get(id);
        if (TERMINAL.contains(t.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_STATUS_INVALID, "终态任务不可暂停");
        }
        t.setStatus("PAUSED");
        t.setUpdatedAt(LocalDateTime.now());
        repository.save(t);
        eventPublisher.publish(id, "TASK_PAUSED", "INFO", "任务已暂停", null);
    }

    public void resume(Long id) {
        AgentTask t = get(id);
        t.setStatus("RUNNING_STEP");
        t.setUpdatedAt(LocalDateTime.now());
        repository.save(t);
        eventPublisher.publish(id, "TASK_RESUMED", "INFO", "任务已恢复", null);
    }

    public void cancel(Long id) {
        AgentTask t = get(id);
        if (TERMINAL.contains(t.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_STATUS_INVALID, "终态任务不可取消");
        }
        t.setStatus("CANCEL_REQUESTED");
        t.setUpdatedAt(LocalDateTime.now());
        repository.save(t);
        eventPublisher.publish(id, "CANCEL_REQUESTED", "WARN", "收到取消请求", null);
    }
}