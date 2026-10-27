package com.webmodel.agent.model.adapter;

import com.microsoft.playwright.Page;

public class ClaudeAdapter implements ModelAdapter {
    @Override
    public String getLoginUrl() { return "https://claude.ai/"; }
    @Override
    public String getPlatform() { return "claude"; }
    @Override
    public LoginCheckResult checkLogin(Page page) { return LoginDetector.detect(page); }
}
