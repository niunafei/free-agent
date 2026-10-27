package com.webmodel.agent.application;

import com.webmodel.agent.controller.McpController;
import com.webmodel.agent.persistence.McpCapability;
import com.webmodel.agent.persistence.McpCapabilityRepository;
import com.webmodel.agent.persistence.McpServerConfig;
import com.webmodel.agent.persistence.McpServerConfigRepository;
import com.webmodel.agent.security.UserContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class McpService {

    private final McpServerConfigRepository serverRepo;
    private final McpCapabilityRepository capabilityRepo;

    public McpService(McpServerConfigRepository serverRepo, McpCapabilityRepository capabilityRepo) {
        this.serverRepo = serverRepo;
        this.capabilityRepo = capabilityRepo;
    }

    public List<McpServerConfig> listServers() {
        return serverRepo.findByUserId(UserContext.getUserId());
    }

    public McpServerConfig create(McpController.McpCreateRequest req) {
        McpServerConfig s = new McpServerConfig();
        s.setUserId(UserContext.getUserId());
        s.setName(req.name());
        s.setTransportType(req.transportType());
        s.setEndpointOrCommandRef(req.endpointOrCommandRef());
        s.setTrustLevel(req.trustLevel() != null ? req.trustLevel() : "LOW");
        s.setEnabled(true);
        s.setConnectionStatus("DISCONNECTED");
        return serverRepo.save(s);
    }

    public void connect(Long id) {
        McpServerConfig s = serverRepo.findById(id).orElseThrow();
        s.setConnectionStatus("CONNECTED");
        s.setLastCheckedAt(java.time.LocalDateTime.now());
        serverRepo.save(s);
    }

    public List<McpCapability> capabilities(Long id) {
        return capabilityRepo.findByServerId(id);
    }

    public void disconnect(Long id) {
        McpServerConfig s = serverRepo.findById(id).orElseThrow();
        s.setConnectionStatus("DISCONNECTED");
        serverRepo.save(s);
    }
}