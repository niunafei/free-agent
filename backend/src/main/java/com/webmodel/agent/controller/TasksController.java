package com.webmodel.agent.controller;

import com.webmodel.agent.application.TaskService;
import com.webmodel.agent.common.PageResult;
import com.webmodel.agent.common.Result;
import com.webmodel.agent.persistence.AgentTask;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TasksController {

    private final TaskService taskService;

    public TasksController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public Result<PageResult<AgentTask>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return Result.success(taskService.page(page, size));
    }

    @GetMapping("/{id}")
    public Result<AgentTask> get(@PathVariable Long id) {
        return Result.success(taskService.get(id));
    }

    @PostMapping
    public Result<AgentTask> create(@Valid @RequestBody TaskCreateRequest req) {
        return Result.success(taskService.create(req));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody ApproveRequest req) {
        taskService.approve(id, req);
        return Result.success();
    }

    @PostMapping("/{id}/pause")
    public Result<Void> pause(@PathVariable Long id) {
        taskService.pause(id);
        return Result.success();
    }

    @PostMapping("/{id}/resume")
    public Result<Void> resume(@PathVariable Long id) {
        taskService.resume(id);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        taskService.cancel(id);
        return Result.success();
    }

    public record TaskCreateRequest(
            String title,
            Long selectedModelId,
            Long conversationId,
            Long skillId) {}

    public record ApproveRequest(String stepId, Integer planVersion, String decision) {}
}