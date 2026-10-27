package com.webmodel.agent.application;

import com.webmodel.agent.browser.BrowserSessionManager;
import com.webmodel.agent.common.BusinessException;
import com.webmodel.agent.common.ErrorCode;
import com.webmodel.agent.controller.ModelsController;
import com.webmodel.agent.model.adapter.ModelAdapter;
import com.webmodel.agent.model.adapter.ModelAdapterFactory;
import com.webmodel.agent.persistence.ModelProfile;
import com.webmodel.agent.persistence.ModelProfileRepository;
import com.webmodel.agent.security.UserContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ModelService {

    private final ModelProfileRepository repository;
    private final BrowserSessionManager browserSessionManager;
    private final ModelAdapterFactory adapterFactory;

    public ModelService(ModelProfileRepository repository,
                        BrowserSessionManager browserSessionManager,
                        ModelAdapterFactory adapterFactory) {
        this.repository = repository;
        this.browserSessionManager = browserSessionManager;
        this.adapterFactory = adapterFactory;
    }

    public List<ModelProfile> listAll() {
        return repository.findByUserId(UserContext.getUserId());
    }

    public ModelProfile create(ModelsController.ModelCreateRequest req) {
        Long userId = UserContext.getUserId();
        if (repository.existsByUserIdAndProviderAndAccountAlias(userId, req.provider(), req.accountAlias())) {
            throw new BusinessException(ErrorCode.CONFLICT, "同平台下账户别名已存在");
        }
        ModelProfile m = new ModelProfile();
        m.setUserId(userId);
        m.setProvider(req.provider());
        m.setDisplayName(req.displayName());
        m.setAccountAlias(req.accountAlias());
        m.setAdapterType(req.adapterType() != null ? req.adapterType() : "WEB_PAGE");
        m.setBaseUrl(req.baseUrl());
        m.setProfileKey(req.provider() + "_" + req.accountAlias());
        m.setEnabled(true);
        m.setStatus("NOT_CONFIGURED");
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());
        return repository.save(m);
    }

    public ModelProfile update(Long id, ModelsController.ModelUpdateRequest req) {
        ModelProfile m = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));
        if (req.displayName() != null) m.setDisplayName(req.displayName());
        if (req.baseUrl() != null) m.setBaseUrl(req.baseUrl());
        if (req.enabled() != null) m.setEnabled(req.enabled());
        m.setUpdatedAt(LocalDateTime.now());
        return repository.save(m);
    }

    public void connect(Long id) {
        ModelProfile m = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));

        String loginUrl = adapterFactory.getLoginUrl(m.getProvider());
        if (loginUrl == null) {
            loginUrl = m.getBaseUrl();
        }
        if (loginUrl == null || loginUrl.isBlank()) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "无法确定登录URL");
        }

        m.setStatus("CONNECTING");
        m.setLoginSessionId(UUID.randomUUID().toString());
        m.setLastCheckedAt(LocalDateTime.now());
        m.setLastErrorCode(null);
        m.setLastErrorMsg(null);
        m.setUpdatedAt(LocalDateTime.now());
        repository.save(m);

        try {
            browserSessionManager.startSession(id, loginUrl);
        } catch (Exception e) {
            m.setStatus("ERROR");
            m.setLastErrorCode("BROWSER_LAUNCH_FAILED");
            m.setLastErrorMsg("浏览器启动失败: " + e.getMessage());
            m.setUpdatedAt(LocalDateTime.now());
            repository.save(m);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "浏览器启动失败，请确认已安装 Playwright 浏览器（执行: playwright install chromium）");
        }
    }

    public ModelProfile check(Long id) {
        ModelProfile m = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));
        m.setLastCheckedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());
        repository.save(m);
        return m;
    }

    public void disconnect(Long id) {
        ModelProfile m = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));
        browserSessionManager.stopSession(id);
        m.setStatus("DISCONNECTED");
        m.setLoginSessionId(null);
        m.setUpdatedAt(LocalDateTime.now());
        repository.save(m);
    }

    public LoginStatusResult loginStatus(Long id) {
        ModelProfile m = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));

        if (!browserSessionManager.hasSession(id)) {
            return new LoginStatusResult(m.getStatus(), "浏览器未启动，请先点击登录", null);
        }

        ModelAdapter adapter = adapterFactory.getAdapter(m.getProvider());
        if (adapter == null) {
            return new LoginStatusResult(m.getStatus(), "不支持的平台: " + m.getProvider(), null);
        }

        BrowserSessionManager.BrowserSession session = browserSessionManager.getSession(id);
        try {
            ModelAdapter.LoginCheckResult result = adapter.checkLogin(session.page);
            if (result.loggedIn()) {
                m.setStatus("READY");
                m.setLastCheckedAt(LocalDateTime.now());
                m.setUpdatedAt(LocalDateTime.now());
                repository.save(m);

                if (!browserSessionManager.isHeadless(id) && browserSessionManager.isHeadlessAfterLogin()) {
                    try {
                        String loginUrl = adapter.getLoginUrl();
                        browserSessionManager.switchToHeadless(id, loginUrl);
                    } catch (Exception ignored) {}
                }

                String msg = browserSessionManager.isHeadless(id) ? "登录成功，已切换为后台运行" : "登录成功";
                return new LoginStatusResult("READY", msg, result.detail());
            } else {
                m.setLastCheckedAt(LocalDateTime.now());
                m.setUpdatedAt(LocalDateTime.now());
                repository.save(m);
                return new LoginStatusResult("WAITING_USER", result.detail(), null);
            }
        } catch (Exception e) {
            m.setStatus("ERROR");
            m.setLastErrorCode("CHECK_FAILED");
            m.setLastErrorMsg(e.getMessage());
            m.setUpdatedAt(LocalDateTime.now());
            repository.save(m);
            return new LoginStatusResult("ERROR", "检测失败: " + e.getMessage(), null);
        }
    }

    public record LoginStatusResult(String status, String message, String detail) {}
}