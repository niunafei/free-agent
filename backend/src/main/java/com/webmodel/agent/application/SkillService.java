package com.webmodel.agent.application;

import com.webmodel.agent.controller.SkillsController;
import com.webmodel.agent.persistence.SkillDefinition;
import com.webmodel.agent.persistence.SkillDefinitionRepository;
import com.webmodel.agent.security.UserContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkillService {

    private final SkillDefinitionRepository repository;

    public SkillService(SkillDefinitionRepository repository) {
        this.repository = repository;
    }

    public List<SkillDefinition> listAll() {
        return repository.findByUserId(UserContext.getUserId());
    }

    public SkillDefinition importSkill(SkillsController.SkillImportRequest req) {
        SkillDefinition s = new SkillDefinition();
        s.setUserId(UserContext.getUserId());
        s.setSkillId(java.util.UUID.randomUUID().toString());
        s.setName("imported");
        s.setVersion("1.0.0");
        s.setSourcePath(req.sourcePath());
        s.setEnabled(true);
        s.setValidationStatus("PENDING");
        return repository.save(s);
    }

    public SkillDefinition update(Long id, SkillsController.SkillUpdateRequest req) {
        SkillDefinition s = repository.findById(id).orElseThrow();
        if (req.enabled() != null) s.setEnabled(req.enabled());
        return repository.save(s);
    }
}