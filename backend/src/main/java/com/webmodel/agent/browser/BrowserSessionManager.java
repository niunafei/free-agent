package com.webmodel.agent.browser;

import com.microsoft.playwright.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BrowserSessionManager {

    private final Map<Long, BrowserSession> sessions = new ConcurrentHashMap<>();

    @Value("${webmodel.playwright.headless-after-login:false}")
    private boolean headlessAfterLogin;

    public boolean isHeadlessAfterLogin() {
        return headlessAfterLogin;
    }

    public static class BrowserSession {
        public Playwright playwright;
        public Browser browser;
        public BrowserContext context;
        public Page page;
        public boolean headless;
    }

    public BrowserSession startSession(Long modelId, String url) {
        stopSession(modelId);
        BrowserSession session = new BrowserSession();
        session.playwright = Playwright.create();
        session.browser = session.playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(false));
        session.context = session.browser.newContext();
        session.page = session.context.newPage();
        session.page.navigate(url);
        session.headless = false;
        sessions.put(modelId, session);
        return session;
    }

    public BrowserSession startHeadlessSession(Long modelId, String url, String storageStateJson) {
        stopSession(modelId);
        BrowserSession session = new BrowserSession();
        session.playwright = Playwright.create();
        session.browser = session.playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true));
        Browser.NewContextOptions options = new Browser.NewContextOptions();
        if (storageStateJson != null) {
            options.setStorageState(storageStateJson);
        }
        session.context = session.browser.newContext(options);
        session.page = session.context.newPage();
        session.page.navigate(url);
        session.headless = true;
        sessions.put(modelId, session);
        return session;
    }

    public String exportStorageState(Long modelId) {
        BrowserSession session = sessions.get(modelId);
        if (session == null) return null;
        return session.context.storageState();
    }

    public String getCurrentUrl(Long modelId) {
        BrowserSession session = sessions.get(modelId);
        if (session == null) return null;
        return session.page.url();
    }

    public void switchToHeadless(Long modelId, String targetUrl) {
        BrowserSession session = sessions.get(modelId);
        if (session == null || session.headless) return;

        String storageState = session.context.storageState();
        String url = targetUrl != null ? targetUrl : session.page.url();

        stopSession(modelId);

        BrowserSession headlessSession = new BrowserSession();
        headlessSession.playwright = Playwright.create();
        headlessSession.browser = headlessSession.playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true));
        headlessSession.context = headlessSession.browser.newContext(
                new Browser.NewContextOptions().setStorageState(storageState));
        headlessSession.page = headlessSession.context.newPage();
        headlessSession.page.navigate(url);
        headlessSession.headless = true;
        sessions.put(modelId, headlessSession);
    }

    public BrowserSession getSession(Long modelId) {
        return sessions.get(modelId);
    }

    public boolean hasSession(Long modelId) {
        return sessions.containsKey(modelId);
    }

    public boolean isHeadless(Long modelId) {
        BrowserSession session = sessions.get(modelId);
        return session != null && session.headless;
    }

    public void stopSession(Long modelId) {
        BrowserSession session = sessions.remove(modelId);
        if (session != null) {
            try { session.context.close(); } catch (Exception ignored) {}
            try { session.browser.close(); } catch (Exception ignored) {}
            try { session.playwright.close(); } catch (Exception ignored) {}
        }
    }

    public void stopAll() {
        sessions.keySet().forEach(this::stopSession);
    }
}