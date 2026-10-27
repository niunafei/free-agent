package com.webmodel.agent.application;

import com.webmodel.agent.controller.ToolsController;
import com.webmodel.agent.persistence.ToolDefinition;
import com.webmodel.agent.persistence.ToolDefinitionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToolService {

    private final ToolDefinitionRepository repository;

    public ToolService(ToolDefinitionRepository repository) {
        this.repository = repository;
    }

    public List<ToolDefinition> listAll() {
        return repository.findAll();
    }

    public ToolDefinition update(Long id, ToolsController.ToolUpdateRequest req) {
        ToolDefinition t = repository.findById(id).orElseThrow();
        if (req.enabled() != null) t.setEnabled(req.enabled());
        if (req.timeoutMs() != null) t.setTimeoutMs(req.timeoutMs());
        return repository.save(t);
    }
}