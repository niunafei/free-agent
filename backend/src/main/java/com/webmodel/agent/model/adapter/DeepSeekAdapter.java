package com.webmodel.agent.model.adapter;

import com.microsoft.playwright.Page;

public class DeepSeekAdapter implements ModelAdapter {
    @Override
    public String getLoginUrl() { return "https://chat.deepseek.com/"; }
    @Override
    public String getPlatform() { return "deepseek"; }
    @Override
    public LoginCheckResult checkLogin(Page page) { return LoginDetector.detect(page); }
}
